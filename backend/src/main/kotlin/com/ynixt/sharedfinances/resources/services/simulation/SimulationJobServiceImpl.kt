package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.application.web.dto.simulationjobs.SimulationJobStatusEventDto
import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.SimulationJobStatus
import com.ynixt.sharedfinances.domain.exceptions.http.SimulationJobForbiddenException
import com.ynixt.sharedfinances.domain.exceptions.http.SimulationJobNotFoundException
import com.ynixt.sharedfinances.domain.queue.producer.SimulationJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.simulation.NewSimulationJobInput
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobProcessor
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.SimulationJobDispatchRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.SimulationJobRepository
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@Service
class SimulationJobServiceImpl(
    private val simulationJobRepository: SimulationJobRepository,
    private val simulationJobDatabaseClientRepository: SimulationJobDispatchRepository,
    private val simulationJobDispatchQueueProducer: SimulationJobDispatchQueueProducer,
    private val simulationJobProcessor: SimulationJobProcessor,
    private val groupPermissionService: GroupPermissionService,
    private val actionEventService: ActionEventService,
    private val clock: Clock,
) : SimulationJobService {
    companion object {
        private const val RETENTION_DAYS = 30L
        private const val READY_OWNER_DISPATCH_BATCH = 500
        private val LEASE_DURATION: Duration = Duration.ofMinutes(1)
        private val HEARTBEAT_INTERVAL: Duration = Duration.ofSeconds(15)
        private val TERMINAL_STATUSES =
            setOf(
                SimulationJobStatus.COMPLETED,
                SimulationJobStatus.FAILED,
                SimulationJobStatus.CANCELLED,
            )
    }

    private val logger = LoggerFactory.getLogger(SimulationJobServiceImpl::class.java)

    override suspend fun create(
        ownerUserId: UUID,
        input: NewSimulationJobInput,
    ): SimulationJobEntity {
        val saved =
            simulationJobRepository
                .save(
                    SimulationJobEntity(
                        ownerUserId = ownerUserId,
                        ownerGroupId = null,
                        requestedByUserId = ownerUserId,
                        type = input.type,
                        status = SimulationJobStatus.QUEUED,
                        requestPayload = input.requestPayload?.trim()?.ifBlank { null },
                        resultPayload = null,
                        errorMessage = null,
                        leaseExpiresAt = null,
                        workerId = null,
                        startedAt = null,
                        finishedAt = null,
                        cancelledAt = null,
                        retries = 0,
                    ),
                ).awaitSingle()

        val jobId = requireNotNull(saved.id) { "simulation job id" }
        dispatchSafely(jobId)
        emitStateEvent(saved, ActionEventType.INSERT)
        return saved
    }

    override suspend fun createForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        input: NewSimulationJobInput,
    ): SimulationJobEntity {
        if (!groupPermissionService.hasPermission(requesterUserId, groupId, GroupPermissions.NEW_SIMULATION)) {
            throw SimulationJobForbiddenException()
        }

        val saved =
            simulationJobRepository
                .save(
                    SimulationJobEntity(
                        ownerUserId = null,
                        ownerGroupId = groupId,
                        requestedByUserId = requesterUserId,
                        type = input.type,
                        status = SimulationJobStatus.QUEUED,
                        requestPayload = input.requestPayload?.trim()?.ifBlank { null },
                        resultPayload = null,
                        errorMessage = null,
                        leaseExpiresAt = null,
                        workerId = null,
                        startedAt = null,
                        finishedAt = null,
                        cancelledAt = null,
                        retries = 0,
                    ),
                ).awaitSingle()

        val jobId = requireNotNull(saved.id) { "simulation job id" }
        dispatchSafely(jobId)
        emitStateEvent(saved, ActionEventType.INSERT)
        return saved
    }

    override suspend fun getForOwner(
        ownerUserId: UUID,
        jobId: UUID,
    ): SimulationJobEntity {
        val entity = loadById(jobId)
        ensureOwnerUser(ownerUserId, entity)
        return entity
    }

    override suspend fun getForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        jobId: UUID,
    ): SimulationJobEntity {
        if (!groupPermissionService.hasPermission(requesterUserId, groupId)) {
            throw SimulationJobForbiddenException()
        }
        val entity = loadById(jobId)
        ensureOwnerGroup(groupId, entity)
        return entity
    }

    override suspend fun listForOwner(
        ownerUserId: UUID,
        pageable: Pageable,
    ): Page<SimulationJobEntity> =
        createPage(
            pageable = pageable,
            countFn = { simulationJobRepository.countByOwnerUserId(ownerUserId) },
        ) {
            simulationJobRepository.findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(ownerUserId, pageable)
        }

    override suspend fun listForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        pageable: Pageable,
    ): Page<SimulationJobEntity> {
        if (!groupPermissionService.hasPermission(requesterUserId, groupId)) {
            throw SimulationJobForbiddenException()
        }
        return createPage(
            pageable = pageable,
            countFn = { simulationJobRepository.countByOwnerGroupId(groupId) },
        ) {
            simulationJobRepository.findAllByOwnerGroupIdOrderByCreatedAtDescIdDesc(groupId, pageable)
        }
    }

    override suspend fun cancelForOwner(
        ownerUserId: UUID,
        jobId: UUID,
    ): SimulationJobEntity {
        val existing = loadById(jobId)
        ensureOwnerUser(ownerUserId, existing)

        if (existing.status in TERMINAL_STATUSES) {
            return existing
        }

        val updated = simulationJobRepository.cancelIfOwnedPending(jobId, ownerUserId).awaitSingle()
        val reloaded = loadById(jobId)

        if (updated > 0) {
            emitStateEvent(reloaded, ActionEventType.UPDATE)
            dispatchNextQueuedForOwner(ownerUserId)
        }

        return reloaded
    }

    override suspend fun cancelForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        jobId: UUID,
    ): SimulationJobEntity {
        if (!groupPermissionService.hasPermission(requesterUserId, groupId, GroupPermissions.NEW_SIMULATION)) {
            throw SimulationJobForbiddenException()
        }

        val existing = loadById(jobId)
        ensureOwnerGroup(groupId, existing)

        if (existing.status in TERMINAL_STATUSES) {
            return existing
        }

        val updated = simulationJobRepository.cancelIfOwnedByGroupPending(jobId, groupId).awaitSingle()
        val reloaded = loadById(jobId)

        if (updated > 0) {
            emitStateEvent(reloaded, ActionEventType.UPDATE)
            dispatchNextQueuedForGroup(groupId)
        }

        return reloaded
    }

    override suspend fun deleteForOwner(
        ownerUserId: UUID,
        jobId: UUID,
    ) {
        val existing = loadById(jobId)
        ensureOwnerUser(ownerUserId, existing)
        if (existing.requestedByUserId != ownerUserId) {
            throw SimulationJobForbiddenException()
        }
        val deleted = simulationJobRepository.deletePersonalIfCreator(jobId, ownerUserId).awaitSingle()
        if (deleted == 0L) {
            throw SimulationJobNotFoundException(jobId)
        }
        emitDeleteEvent(existing)
        dispatchNextQueuedForOwner(ownerUserId)
    }

    override suspend fun deleteForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        jobId: UUID,
    ) {
        if (!groupPermissionService.hasPermission(requesterUserId, groupId, GroupPermissions.DELETE_SIMULATIONS)) {
            throw SimulationJobForbiddenException()
        }
        val existing = loadById(jobId)
        ensureOwnerGroup(groupId, existing)
        val deleted = simulationJobRepository.deleteGroupJob(jobId, groupId).awaitSingle()
        if (deleted == 0L) {
            throw SimulationJobNotFoundException(jobId)
        }
        emitDeleteEvent(existing)
        dispatchNextQueuedForGroup(groupId)
    }

    override suspend fun processDispatchMessage(jobId: UUID) {
        val referenced = simulationJobRepository.findById(jobId.toString()).awaitSingleOrNull() ?: return
        if (referenced.status in TERMINAL_STATUSES) {
            return
        }

        val scope = resolveScope(referenced) ?: return
        val workerId = "sim-worker-${UUID.randomUUID()}"
        val running = claimNextQueuedForScope(scope, workerId) ?: return
        emitStateEvent(running, ActionEventType.UPDATE)

        processRunningJob(
            running = running,
            workerId = workerId,
        )

        dispatchNextQueuedForScope(scope)
    }

    override suspend fun dispatchNextQueuedForOwner(ownerUserId: UUID) {
        dispatchNextQueuedForScope(JobOwnerScope.User(ownerUserId))
    }

    override suspend fun dispatchNextQueuedForGroup(ownerGroupId: UUID) {
        dispatchNextQueuedForScope(JobOwnerScope.Group(ownerGroupId))
    }

    override suspend fun reconcileExpiredLeases(): Long {
        val changedRows = simulationJobRepository.reconcileExpiredLeases(OffsetDateTime.now(clock)).awaitSingle()
        dispatchReadyOwners()
        return changedRows
    }

    override suspend fun purgeOldJobs(): Long =
        simulationJobRepository
            .deleteAllByCreatedAtBefore(OffsetDateTime.now(clock).minusDays(RETENTION_DAYS))
            .awaitSingle()

    override suspend fun cancelAndRemoveAllJobsLinkedToUserForCompliance(userId: UUID) {
        val scopes =
            simulationJobDatabaseClientRepository
                .findDispatchScopesForPendingJobsLinkedToUser(userId)
                .collectList()
                .awaitSingle()

        simulationJobRepository.cancelAllPendingLinkedToUser(userId).awaitSingle()
        simulationJobRepository.deleteAllLinkedToUser(userId).awaitSingle()

        val userOwners = scopes.mapNotNull { it.ownerUserId }.toSet()
        val groupOwners = scopes.mapNotNull { it.ownerGroupId }.toSet()

        for (ownerId in userOwners) {
            dispatchNextQueuedForOwner(ownerId)
        }
        for (groupId in groupOwners) {
            dispatchNextQueuedForGroup(groupId)
        }
    }

    private suspend fun claimNextQueuedForScope(
        scope: JobOwnerScope,
        workerId: String,
    ): SimulationJobEntity? {
        val now = OffsetDateTime.now(clock)
        val promotedId =
            when (scope) {
                is JobOwnerScope.User ->
                    simulationJobDatabaseClientRepository
                        .promoteOldestQueuedUserJobToRunning(
                            ownerUserId = scope.ownerUserId,
                            workerId = workerId,
                            now = now,
                            leaseExpiresAt = now.plus(LEASE_DURATION),
                        ).awaitSingleOrNull()

                is JobOwnerScope.Group ->
                    simulationJobDatabaseClientRepository
                        .promoteOldestQueuedGroupJobToRunning(
                            ownerGroupId = scope.ownerGroupId,
                            workerId = workerId,
                            now = now,
                            leaseExpiresAt = now.plus(LEASE_DURATION),
                        ).awaitSingleOrNull()
            } ?: return null

        return simulationJobRepository.findById(promotedId.toString()).awaitSingleOrNull()
    }

    private suspend fun processRunningJob(
        running: SimulationJobEntity,
        workerId: String,
    ) = coroutineScope {
        val jobId = requireNotNull(running.id) { "simulation job id" }
        val heartbeat =
            launch {
                renewLeaseLoop(jobId, workerId)
            }

        try {
            val fresh = simulationJobRepository.findById(jobId.toString()).awaitSingleOrNull() ?: return@coroutineScope
            if (fresh.status != SimulationJobStatus.RUNNING || fresh.workerId != workerId) {
                return@coroutineScope
            }

            val resultPayload = simulationJobProcessor.process(fresh)
            val completed =
                simulationJobDatabaseClientRepository
                    .markCompleted(
                        jobId = jobId,
                        workerId = workerId,
                        resultPayload = resultPayload,
                        finishedAt = OffsetDateTime.now(clock),
                    ).awaitSingle()

            if (completed > 0) {
                simulationJobRepository
                    .findById(jobId.toString())
                    .awaitSingleOrNull()
                    ?.let { emitStateEvent(it, ActionEventType.UPDATE) }
            }
        } catch (e: Exception) {
            logger.error("Simulation job $jobId failed", e)
            val failed =
                simulationJobDatabaseClientRepository
                    .markFailed(
                        jobId = jobId,
                        workerId = workerId,
                        errorMessage = sanitizeErrorMessage(e),
                        finishedAt = OffsetDateTime.now(clock),
                    ).awaitSingle()

            if (failed > 0) {
                simulationJobRepository
                    .findById(jobId.toString())
                    .awaitSingleOrNull()
                    ?.let { emitStateEvent(it, ActionEventType.UPDATE) }
            }
        } finally {
            heartbeat.cancelAndJoin()
        }
    }

    private suspend fun renewLeaseLoop(
        jobId: UUID,
        workerId: String,
    ) {
        while (currentCoroutineContext().isActive) {
            delay(HEARTBEAT_INTERVAL.toMillis())
            val renewed =
                simulationJobDatabaseClientRepository
                    .renewLease(
                        jobId = jobId,
                        workerId = workerId,
                        leaseExpiresAt = OffsetDateTime.now(clock).plus(LEASE_DURATION),
                    ).awaitSingle()

            if (renewed == 0L) {
                return
            }
        }
    }

    private suspend fun dispatchReadyOwners() {
        val userOwners =
            simulationJobDatabaseClientRepository
                .findUserOwnersReadyForDispatch(READY_OWNER_DISPATCH_BATCH)
                .collectList()
                .awaitSingle()

        for (ownerId in userOwners) {
            dispatchNextQueuedForScope(JobOwnerScope.User(ownerId))
        }

        val groupOwners =
            simulationJobDatabaseClientRepository
                .findGroupOwnersReadyForDispatch(READY_OWNER_DISPATCH_BATCH)
                .collectList()
                .awaitSingle()

        for (groupId in groupOwners) {
            dispatchNextQueuedForScope(JobOwnerScope.Group(groupId))
        }
    }

    private suspend fun dispatchNextQueuedForScope(scope: JobOwnerScope) {
        val nextId =
            when (scope) {
                is JobOwnerScope.User ->
                    simulationJobDatabaseClientRepository
                        .findOldestQueuedUserJobId(
                            scope.ownerUserId,
                        ).awaitSingleOrNull()
                is JobOwnerScope.Group ->
                    simulationJobDatabaseClientRepository.findOldestQueuedGroupJobId(scope.ownerGroupId).awaitSingleOrNull()
            } ?: return
        dispatchSafely(nextId)
    }

    private fun dispatchSafely(jobId: UUID) {
        runCatching {
            simulationJobDispatchQueueProducer.send(jobId)
        }.onFailure { ex ->
            logger.error("Failed to publish simulation job dispatch for $jobId", ex)
        }
    }

    private suspend fun emitDeleteEvent(entity: SimulationJobEntity) {
        val jobId = requireNotNull(entity.id) { "simulation job id" }
        val groupId = entity.ownerGroupId
        runCatching {
            actionEventService.newEvent(
                userId = entity.ownerUserId ?: entity.requestedByUserId,
                type = ActionEventType.DELETE,
                category = ActionEventCategory.SIMULATION_JOB,
                data = jobId.toString(),
                groupInfo = groupId?.let { NewEventGroupInfo(it) },
            )
        }.onFailure { ex ->
            logger.warn("Failed to emit simulation job delete event for $jobId", ex)
        }
    }

    private suspend fun emitStateEvent(
        entity: SimulationJobEntity,
        eventType: ActionEventType,
    ) {
        val event =
            SimulationJobStatusEventDto(
                id = requireNotNull(entity.id) { "simulation job id" },
                type = entity.type,
                status = entity.status,
                resultPayload = entity.resultPayload,
                errorMessage = entity.errorMessage,
                finishedAt = entity.finishedAt,
                cancelledAt = entity.cancelledAt,
                retries = entity.retries,
            )

        runCatching {
            val groupId = entity.ownerGroupId
            val ownerUserId = entity.ownerUserId
            actionEventService.newEvent(
                userId = ownerUserId ?: entity.requestedByUserId,
                type = eventType,
                category = ActionEventCategory.SIMULATION_JOB,
                data = event,
                groupInfo = groupId?.let { NewEventGroupInfo(it) },
            )
        }.onFailure { ex ->
            logger.warn("Failed to emit simulation job event for ${entity.id}", ex)
        }
    }

    private suspend fun loadById(jobId: UUID): SimulationJobEntity =
        simulationJobRepository.findById(jobId.toString()).awaitSingleOrNull() ?: throw SimulationJobNotFoundException(jobId)

    private fun ensureOwnerUser(
        ownerUserId: UUID,
        entity: SimulationJobEntity,
    ) {
        if (entity.ownerUserId != ownerUserId || entity.ownerGroupId != null) {
            throw SimulationJobForbiddenException()
        }
    }

    private fun ensureOwnerGroup(
        ownerGroupId: UUID,
        entity: SimulationJobEntity,
    ) {
        if (entity.ownerGroupId != ownerGroupId || entity.ownerUserId != null) {
            throw SimulationJobForbiddenException()
        }
    }

    private fun resolveScope(entity: SimulationJobEntity): JobOwnerScope? =
        when {
            entity.ownerUserId != null -> JobOwnerScope.User(entity.ownerUserId)
            entity.ownerGroupId != null -> JobOwnerScope.Group(entity.ownerGroupId)
            else -> null
        }

    private sealed interface JobOwnerScope {
        data class User(
            val ownerUserId: UUID,
        ) : JobOwnerScope

        data class Group(
            val ownerGroupId: UUID,
        ) : JobOwnerScope
    }

    private fun sanitizeErrorMessage(error: Throwable): String {
        val message = error.message?.trim().takeUnless { it.isNullOrBlank() } ?: error::class.simpleName ?: "Unexpected error"
        return message.take(1000)
    }
}
