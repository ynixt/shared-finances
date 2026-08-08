package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.queue.producer.ImportJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.domain.services.imports.ImportJobService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ImportBatchDispatchRepository
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ImportJobServiceImpl(
    private val importBatchRepository: ImportBatchRepository,
    private val dispatchRepository: ImportBatchDispatchRepository,
    private val dispatchQueueProducer: ImportJobDispatchQueueProducer,
    private val executionService: ImportBatchExecutionService,
    private val undoExecutionService: ImportBatchUndoExecutionService,
    private val eventPublisher: ImportBatchEventPublisher,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) : ImportJobService {
    private val logger = LoggerFactory.getLogger(ImportJobServiceImpl::class.java)

    override suspend fun processDispatchMessage(batchId: UUID) {
        val referenced = importBatchRepository.findById(batchId).awaitSingleOrNull() ?: return
        if (referenced.status in TERMINAL_STATUSES) {
            return
        }

        val workerId = "import-worker-${UUID.randomUUID()}"
        val running = claimNextQueuedForUser(referenced.userId, workerId) ?: return
        recordQueueTime(running)
        eventPublisher.publish(running, ActionEventType.UPDATE)
        processRunningBatch(running, workerId)
        dispatchNextQueuedForUser(referenced.userId)
    }

    override suspend fun dispatchNextQueuedForUser(userId: UUID) {
        val nextId = dispatchRepository.findOldestQueuedBatchId(userId).awaitSingleOrNull() ?: return
        dispatchSafely(nextId)
    }

    override suspend fun reconcile(): Long {
        val recoveredIds =
            dispatchRepository
                .recoverExpiredLeases(OffsetDateTime.now(clock), MAX_RETRIES)
                .collectList()
                .awaitSingle()
        recoveredIds.forEach { batchId ->
            importBatchRepository.findById(batchId).awaitSingleOrNull()?.let { batch ->
                meterRegistry.counter("imports.leases.recovered", "status", batch.status.name).increment()
                eventPublisher.publish(batch, ActionEventType.UPDATE)
            }
        }

        val readyUsers = dispatchRepository.findUsersReadyForDispatch(READY_USER_DISPATCH_BATCH).collectList().awaitSingle()
        readyUsers.forEach { dispatchNextQueuedForUser(it) }
        logger.info("Import reconciliation recovered {} lease(s) and dispatched {} user queue(s)", recoveredIds.size, readyUsers.size)
        return recoveredIds.size.toLong()
    }

    private suspend fun claimNextQueuedForUser(
        userId: UUID,
        workerId: String,
    ): ImportBatchEntity? {
        val now = OffsetDateTime.now(clock)
        val claimedId =
            dispatchRepository
                .claimOldestQueuedForUser(
                    userId = userId,
                    workerId = workerId,
                    now = now,
                    leaseExpiresAt = now.plus(LEASE_DURATION),
                ).awaitSingleOrNull() ?: return null
        return importBatchRepository.findById(claimedId).awaitSingleOrNull()
    }

    private suspend fun processRunningBatch(
        running: ImportBatchEntity,
        workerId: String,
    ) = coroutineScope {
        val batchId = requireNotNull(running.id) { "import batch id" }
        val started = OffsetDateTime.now(clock)
        val heartbeat = launch { renewLeaseLoop(batchId, workerId) }

        try {
            when (running.status) {
                ImportBatchStatus.RUNNING -> {
                    executionService.execute(batchId, workerId)
                    val completed = importBatchRepository.findById(batchId).awaitSingle()
                    meterRegistry.counter("imports.terminal", "result", "completed").increment()
                    recordDuration(started, "completed")
                    logger.info(
                        "Import batch {} completed in {} ms after {} attempt(s)",
                        batchId,
                        elapsedMillis(started),
                        completed.retries,
                    )
                    eventPublisher.publish(completed, ActionEventType.UPDATE)
                }

                ImportBatchStatus.UNDO_RUNNING -> {
                    undoExecutionService.execute(batchId, workerId)
                    meterRegistry.counter("imports.terminal", "result", "undone").increment()
                    recordDuration(started, "undone")
                    logger.info("Import batch {} undone in {} ms after {} attempt(s)", batchId, elapsedMillis(started), running.retries)
                    eventPublisher.publishDeleted(running)
                }

                else -> error("Claimed import batch has an invalid running status.")
            }
        } catch (error: Exception) {
            handleProcessingFailure(running, workerId, started, error)
        } finally {
            heartbeat.cancelAndJoin()
        }
    }

    private suspend fun handleProcessingFailure(
        running: ImportBatchEntity,
        workerId: String,
        started: OffsetDateTime,
        error: Exception,
    ) {
        val batchId = requireNotNull(running.id) { "import batch id" }
        val undo = running.status == ImportBatchStatus.UNDO_RUNNING
        val safeMessage = sanitizeErrorMessage(error, undo)
        val terminal = error is IllegalArgumentException || running.retries >= MAX_RETRIES
        recordDuration(started, if (terminal) "failed" else "retry")
        logger.error(
            "Import batch {} attempt {} failed after {} ms (terminal={}, errorType={})",
            batchId,
            running.retries,
            elapsedMillis(started),
            terminal,
            error::class.simpleName,
        )

        val changed =
            if (terminal) {
                dispatchRepository
                    .markFailed(batchId, workerId, safeMessage, OffsetDateTime.now(clock))
                    .awaitSingle()
            } else {
                dispatchRepository.markQueuedForRetry(batchId, workerId, safeMessage).awaitSingle()
            }
        if (changed == 0L) {
            return
        }

        val updated = importBatchRepository.findById(batchId).awaitSingle()
        meterRegistry.counter("imports.retries", "terminal", terminal.toString()).increment()
        if (terminal) {
            meterRegistry.counter("imports.terminal", "result", if (undo) "undo_failed" else "failed").increment()
        }
        eventPublisher.publish(updated, ActionEventType.UPDATE)
    }

    private suspend fun renewLeaseLoop(
        batchId: UUID,
        workerId: String,
    ) {
        while (currentCoroutineContext().isActive) {
            delay(HEARTBEAT_INTERVAL.toMillis())
            val renewed =
                dispatchRepository
                    .renewLease(batchId, workerId, OffsetDateTime.now(clock).plus(LEASE_DURATION))
                    .awaitSingle()
            if (renewed == 0L) {
                return
            }
        }
    }

    private fun dispatchSafely(batchId: UUID) {
        runCatching { dispatchQueueProducer.send(batchId) }
            .onFailure { error ->
                logger.error(
                    "Failed to publish import batch dispatch for {} (errorType={})",
                    batchId,
                    error::class.simpleName,
                )
            }
    }

    private fun recordQueueTime(batch: ImportBatchEntity) {
        if (batch.status == ImportBatchStatus.UNDO_RUNNING) return
        val createdAt = batch.createdAt ?: return
        meterRegistry.timer("imports.queue.time").record(Duration.between(createdAt, OffsetDateTime.now(clock)))
    }

    private fun recordDuration(
        started: OffsetDateTime,
        result: String,
    ) {
        meterRegistry.timer("imports.processing.duration", "result", result).record(Duration.between(started, OffsetDateTime.now(clock)))
    }

    private fun elapsedMillis(started: OffsetDateTime): Long = Duration.between(started, OffsetDateTime.now(clock)).toMillis()

    private fun sanitizeErrorMessage(
        error: Throwable,
        undo: Boolean,
    ): String {
        val safe =
            when {
                undo -> "A importação não pôde ser desfeita. Tente novamente mais tarde."
                error is IllegalArgumentException -> "Os dados da importação são inválidos."
                else -> "A importação não pôde ser concluída. Tente enviar o arquivo novamente mais tarde."
            }
        return safe.take(MAX_ERROR_LENGTH)
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val MAX_ERROR_LENGTH = 500
        const val READY_USER_DISPATCH_BATCH = 500
        val LEASE_DURATION: Duration = Duration.ofMinutes(1)
        val HEARTBEAT_INTERVAL: Duration = Duration.ofSeconds(15)
        val TERMINAL_STATUSES =
            setOf(
                ImportBatchStatus.COMPLETED,
                ImportBatchStatus.FAILED,
                ImportBatchStatus.UNDO_FAILED,
            )
    }
}
