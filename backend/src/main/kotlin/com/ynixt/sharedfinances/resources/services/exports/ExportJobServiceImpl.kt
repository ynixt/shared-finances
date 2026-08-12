package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import com.ynixt.sharedfinances.domain.queue.producer.ExportJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.repositories.ExportBatchRepository
import com.ynixt.sharedfinances.domain.services.exports.ExportJobService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ExportBatchDispatchRepository
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
class ExportJobServiceImpl(
    private val batchRepository: ExportBatchRepository,
    private val dispatchRepository: ExportBatchDispatchRepository,
    private val queueProducer: ExportJobDispatchQueueProducer,
    private val executionService: ExportBatchExecutionService,
    private val eventPublisher: ExportBatchEventPublisher,
    private val quotaService: PlanQuotaService,
    private val clock: Clock,
) : ExportJobService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun processDispatchMessage(batchId: UUID) {
        val referenced = batchRepository.findById(batchId).awaitSingleOrNull() ?: return
        if (referenced.status in TERMINAL_STATUSES) return
        val workerId = "export-worker-${UUID.randomUUID()}"
        val now = OffsetDateTime.now(clock)
        val claimedId =
            dispatchRepository
                .claimOldestQueuedForUser(
                    referenced.userId,
                    workerId,
                    now,
                    now.plus(LEASE_DURATION),
                ).awaitSingleOrNull() ?: return
        val running = batchRepository.findById(claimedId).awaitSingle()
        eventPublisher.publish(running, ActionEventType.UPDATE)
        processRunning(running, workerId)
        dispatchNextQueuedForUser(referenced.userId)
    }

    override suspend fun dispatchNextQueuedForUser(userId: UUID) {
        dispatchRepository.findOldestQueuedBatchId(userId).awaitSingleOrNull()?.let(::dispatchSafely)
    }

    override suspend fun reconcile(): Long {
        val recovered = dispatchRepository.recoverExpiredLeases(OffsetDateTime.now(clock), MAX_RETRIES).collectList().awaitSingle()
        recovered.forEach { id ->
            batchRepository.findById(id).awaitSingleOrNull()?.let { eventPublisher.publish(it, ActionEventType.UPDATE) }
        }
        dispatchRepository
            .findUsersReadyForDispatch(500)
            .collectList()
            .awaitSingle()
            .forEach { dispatchNextQueuedForUser(it) }
        return recovered.size.toLong()
    }

    private suspend fun processRunning(
        running: com.ynixt.sharedfinances.domain.entities.exports.ExportBatchEntity,
        workerId: String,
    ) = coroutineScope {
        val batchId = requireNotNull(running.id)
        val heartbeat = launch { renewLeaseLoop(batchId, workerId) }
        try {
            executionService.execute(batchId, workerId)
            val completed = batchRepository.findById(batchId).awaitSingle()
            eventPublisher.publish(completed, ActionEventType.UPDATE)
            quotaService.usageChanged(completed.userId, PlanLimitKey.EXPORTS_PER_MONTH)
        } catch (error: Exception) {
            val terminal = error is AppResponseException || error is IllegalArgumentException || running.retries >= MAX_RETRIES
            val message = (error.message ?: "The export could not be completed.").take(500)
            val changed =
                if (terminal) {
                    dispatchRepository.markFailed(batchId, workerId, message, OffsetDateTime.now(clock)).awaitSingle()
                } else {
                    dispatchRepository.markQueuedForRetry(batchId, workerId, message).awaitSingle()
                }
            if (changed == 1L) {
                batchRepository.findById(batchId).awaitSingleOrNull()?.let { eventPublisher.publish(it, ActionEventType.UPDATE) }
            }
            logger.error("Export batch {} failed (terminal={})", batchId, terminal, error)
        } finally {
            heartbeat.cancelAndJoin()
        }
    }

    private suspend fun renewLeaseLoop(
        batchId: UUID,
        workerId: String,
    ) {
        while (currentCoroutineContext().isActive) {
            delay(HEARTBEAT_INTERVAL.toMillis())
            if (dispatchRepository.renewLease(batchId, workerId, OffsetDateTime.now(clock).plus(LEASE_DURATION)).awaitSingle() == 0L) return
        }
    }

    private fun dispatchSafely(batchId: UUID) {
        runCatching { queueProducer.send(batchId) }
            .onFailure { logger.error("Failed to publish export batch dispatch for {}", batchId, it) }
    }

    private companion object {
        const val MAX_RETRIES = 3
        val LEASE_DURATION: Duration = Duration.ofMinutes(1)
        val HEARTBEAT_INTERVAL: Duration = Duration.ofSeconds(15)
        val TERMINAL_STATUSES = setOf(ExportBatchStatus.COMPLETED, ExportBatchStatus.FAILED, ExportBatchStatus.EXPIRED)
    }
}
