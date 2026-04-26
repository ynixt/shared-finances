package com.ynixt.sharedfinances.scenarios.simulation.support

import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.SimulationJobStatus
import com.ynixt.sharedfinances.domain.queue.producer.SimulationJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobProcessor
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.SimulationJobDispatchRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.UserLinkedSimulationJobDispatchScope
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.SimulationJobRepository
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.OffsetDateTime
import java.util.ArrayDeque
import java.util.UUID

internal class InMemorySimulationJobStore(
    private val clock: Clock,
) : SimulationJobRepository,
    SimulationJobDispatchRepository {
    private val jobs = linkedMapOf<UUID, SimulationJobEntity>()

    override fun save(entity: SimulationJobEntity): Mono<SimulationJobEntity> =
        Mono.fromSupplier {
            synchronized(jobs) {
                val now = OffsetDateTime.now(clock)
                val resolvedId = entity.id ?: UUID.randomUUID()
                val existing = jobs[resolvedId]
                val stored =
                    entity.copyEntity(
                        id = resolvedId,
                        createdAt = entity.createdAt ?: existing?.createdAt ?: now,
                        updatedAt = now,
                    )
                jobs[resolvedId] = stored
                stored.copyEntity()
            }
        }

    override fun findById(id: String): Mono<SimulationJobEntity> =
        Mono.justOrEmpty(
            synchronized(jobs) {
                parseUuid(id)?.let { jobs[it]?.copyEntity() }
            },
        )

    override fun findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(
        ownerUserId: UUID,
        pageable: Pageable,
    ): Flux<SimulationJobEntity> =
        Flux.fromIterable(
            synchronized(jobs) {
                paginate(
                    jobs.values
                        .asSequence()
                        .filter { it.ownerUserId == ownerUserId && it.ownerGroupId == null }
                        .sortedWith(createdAtDescComparator)
                        .map { it.copyEntity() }
                        .toList(),
                    pageable,
                )
            },
        )

    override fun findAllByOwnerGroupIdOrderByCreatedAtDescIdDesc(
        ownerGroupId: UUID,
        pageable: Pageable,
    ): Flux<SimulationJobEntity> =
        Flux.fromIterable(
            synchronized(jobs) {
                paginate(
                    jobs.values
                        .asSequence()
                        .filter { it.ownerGroupId == ownerGroupId && it.ownerUserId == null }
                        .sortedWith(createdAtDescComparator)
                        .map { it.copyEntity() }
                        .toList(),
                    pageable,
                )
            },
        )

    override fun countByOwnerUserId(ownerUserId: UUID): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                jobs.values.count { it.ownerUserId == ownerUserId && it.ownerGroupId == null }.toLong()
            },
        )

    override fun countByOwnerGroupId(ownerGroupId: UUID): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                jobs.values.count { it.ownerGroupId == ownerGroupId && it.ownerUserId == null }.toLong()
            },
        )

    override fun cancelIfOwnedPending(
        id: UUID,
        ownerUserId: UUID,
    ): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                updateById(id) { existing ->
                    if (
                        existing.ownerUserId == ownerUserId &&
                        existing.ownerGroupId == null &&
                        existing.status in pendingStatuses
                    ) {
                        val now = OffsetDateTime.now(clock)
                        existing.copyEntity(
                            status = SimulationJobStatus.CANCELLED,
                            cancelledAt = now,
                            leaseExpiresAt = null,
                            workerId = null,
                            finishedAt = now,
                            updatedAt = now,
                        )
                    } else {
                        null
                    }
                }
            },
        )

    override fun cancelIfOwnedByGroupPending(
        id: UUID,
        ownerGroupId: UUID,
    ): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                updateById(id) { existing ->
                    if (
                        existing.ownerGroupId == ownerGroupId &&
                        existing.ownerUserId == null &&
                        existing.status in pendingStatuses
                    ) {
                        val now = OffsetDateTime.now(clock)
                        existing.copyEntity(
                            status = SimulationJobStatus.CANCELLED,
                            cancelledAt = now,
                            leaseExpiresAt = null,
                            workerId = null,
                            finishedAt = now,
                            updatedAt = now,
                        )
                    } else {
                        null
                    }
                }
            },
        )

    override fun reconcileExpiredLeases(now: OffsetDateTime): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                jobs.entries.fold(0L) { changed, (id, job) ->
                    if (job.status == SimulationJobStatus.RUNNING && job.leaseExpiresAt?.isBefore(now) == true) {
                        jobs[id] =
                            job.copyEntity(
                                status = SimulationJobStatus.QUEUED,
                                leaseExpiresAt = null,
                                workerId = null,
                                startedAt = null,
                                updatedAt = now,
                            )
                        changed + 1
                    } else {
                        changed
                    }
                }
            },
        )

    override fun deleteAllByCreatedAtBefore(threshold: OffsetDateTime): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                deleteWhere { it.createdAt?.isBefore(threshold) == true }
            },
        )

    override fun deletePersonalIfCreator(
        id: UUID,
        userId: UUID,
    ): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                val entity = jobs[id] ?: return@synchronized 0L
                if (entity.ownerUserId == userId && entity.ownerGroupId == null && entity.requestedByUserId == userId) {
                    jobs.remove(id)
                    1L
                } else {
                    0L
                }
            },
        )

    override fun deleteGroupJob(
        id: UUID,
        groupId: UUID,
    ): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                val entity = jobs[id] ?: return@synchronized 0L
                if (entity.ownerGroupId == groupId && entity.ownerUserId == null) {
                    jobs.remove(id)
                    1L
                } else {
                    0L
                }
            },
        )

    override fun cancelAllPendingLinkedToUser(userId: UUID): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                val now = OffsetDateTime.now(clock)
                jobs.entries.fold(0L) { changed, (id, job) ->
                    if (
                        (job.ownerUserId == userId || job.requestedByUserId == userId) &&
                        job.status in pendingStatuses
                    ) {
                        jobs[id] =
                            job.copyEntity(
                                status = SimulationJobStatus.CANCELLED,
                                cancelledAt = now,
                                leaseExpiresAt = null,
                                workerId = null,
                                finishedAt = now,
                                updatedAt = now,
                            )
                        changed + 1
                    } else {
                        changed
                    }
                }
            },
        )

    override fun deleteAllLinkedToUser(userId: UUID): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                deleteWhere { it.ownerUserId == userId || it.requestedByUserId == userId }
            },
        )

    override fun findDispatchScopesForPendingJobsLinkedToUser(userId: UUID): Flux<UserLinkedSimulationJobDispatchScope> =
        Flux.fromIterable(
            synchronized(jobs) {
                jobs.values
                    .asSequence()
                    .filter { (it.ownerUserId == userId || it.requestedByUserId == userId) && it.status in pendingStatuses }
                    .map { UserLinkedSimulationJobDispatchScope(ownerUserId = it.ownerUserId, ownerGroupId = it.ownerGroupId) }
                    .distinct()
                    .toList()
            },
        )

    override fun promoteOldestQueuedUserJobToRunning(
        ownerUserId: UUID,
        workerId: String,
        now: OffsetDateTime,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<UUID> =
        Mono.justOrEmpty(
            synchronized(jobs) {
                if (jobs.values.any {
                        it.ownerUserId == ownerUserId && it.ownerGroupId == null && it.status == SimulationJobStatus.RUNNING
                    }
                ) {
                    return@synchronized null
                }

                val next =
                    jobs.values
                        .asSequence()
                        .filter { it.ownerUserId == ownerUserId && it.ownerGroupId == null && it.status == SimulationJobStatus.QUEUED }
                        .minWithOrNull(createdAtAscComparator)
                        ?: return@synchronized null

                val nextId = requireNotNull(next.id)
                jobs[nextId] =
                    next.copyEntity(
                        status = SimulationJobStatus.RUNNING,
                        startedAt = next.startedAt ?: now,
                        leaseExpiresAt = leaseExpiresAt,
                        workerId = workerId,
                        retries = next.retries + 1,
                        updatedAt = now,
                    )
                nextId
            },
        )

    override fun promoteOldestQueuedGroupJobToRunning(
        ownerGroupId: UUID,
        workerId: String,
        now: OffsetDateTime,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<UUID> =
        Mono.justOrEmpty(
            synchronized(jobs) {
                if (jobs.values.any {
                        it.ownerGroupId == ownerGroupId && it.ownerUserId == null && it.status == SimulationJobStatus.RUNNING
                    }
                ) {
                    return@synchronized null
                }

                val next =
                    jobs.values
                        .asSequence()
                        .filter { it.ownerGroupId == ownerGroupId && it.ownerUserId == null && it.status == SimulationJobStatus.QUEUED }
                        .minWithOrNull(createdAtAscComparator)
                        ?: return@synchronized null

                val nextId = requireNotNull(next.id)
                jobs[nextId] =
                    next.copyEntity(
                        status = SimulationJobStatus.RUNNING,
                        startedAt = next.startedAt ?: now,
                        leaseExpiresAt = leaseExpiresAt,
                        workerId = workerId,
                        retries = next.retries + 1,
                        updatedAt = now,
                    )
                nextId
            },
        )

    override fun findOldestQueuedUserJobId(ownerUserId: UUID): Mono<UUID> =
        Mono.justOrEmpty(
            synchronized(jobs) {
                jobs.values
                    .asSequence()
                    .filter { it.ownerUserId == ownerUserId && it.ownerGroupId == null && it.status == SimulationJobStatus.QUEUED }
                    .minWithOrNull(createdAtAscComparator)
                    ?.id
            },
        )

    override fun findOldestQueuedGroupJobId(ownerGroupId: UUID): Mono<UUID> =
        Mono.justOrEmpty(
            synchronized(jobs) {
                jobs.values
                    .asSequence()
                    .filter { it.ownerGroupId == ownerGroupId && it.ownerUserId == null && it.status == SimulationJobStatus.QUEUED }
                    .minWithOrNull(createdAtAscComparator)
                    ?.id
            },
        )

    override fun findUserOwnersReadyForDispatch(limit: Int): Flux<UUID> =
        Flux.fromIterable(
            synchronized(jobs) {
                jobs.values
                    .asSequence()
                    .filter { it.ownerUserId != null && it.ownerGroupId == null && it.status == SimulationJobStatus.QUEUED }
                    .mapNotNull { it.ownerUserId }
                    .distinct()
                    .filterNot { ownerId ->
                        jobs.values.any {
                            it.ownerUserId == ownerId &&
                                it.ownerGroupId == null &&
                                it.status == SimulationJobStatus.RUNNING
                        }
                    }.sortedBy(UUID::toString)
                    .take(limit)
                    .toList()
            },
        )

    override fun findGroupOwnersReadyForDispatch(limit: Int): Flux<UUID> =
        Flux.fromIterable(
            synchronized(jobs) {
                jobs.values
                    .asSequence()
                    .filter { it.ownerGroupId != null && it.ownerUserId == null && it.status == SimulationJobStatus.QUEUED }
                    .mapNotNull { it.ownerGroupId }
                    .distinct()
                    .filterNot { groupId ->
                        jobs.values.any {
                            it.ownerGroupId == groupId &&
                                it.ownerUserId == null &&
                                it.status == SimulationJobStatus.RUNNING
                        }
                    }.sortedBy(UUID::toString)
                    .take(limit)
                    .toList()
            },
        )

    override fun renewLease(
        jobId: UUID,
        workerId: String,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                updateById(jobId) { existing ->
                    if (existing.status == SimulationJobStatus.RUNNING && existing.workerId == workerId) {
                        existing.copyEntity(
                            leaseExpiresAt = leaseExpiresAt,
                            updatedAt = OffsetDateTime.now(clock),
                        )
                    } else {
                        null
                    }
                }
            },
        )

    override fun markCompleted(
        jobId: UUID,
        workerId: String,
        resultPayload: String?,
        finishedAt: OffsetDateTime,
    ): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                updateById(jobId) { existing ->
                    if (existing.status == SimulationJobStatus.RUNNING && existing.workerId == workerId) {
                        existing.copyEntity(
                            status = SimulationJobStatus.COMPLETED,
                            resultPayload = resultPayload,
                            errorMessage = null,
                            leaseExpiresAt = null,
                            workerId = null,
                            finishedAt = finishedAt,
                            updatedAt = OffsetDateTime.now(clock),
                        )
                    } else {
                        null
                    }
                }
            },
        )

    override fun markFailed(
        jobId: UUID,
        workerId: String,
        errorMessage: String,
        finishedAt: OffsetDateTime,
    ): Mono<Long> =
        Mono.just(
            synchronized(jobs) {
                updateById(jobId) { existing ->
                    if (existing.status == SimulationJobStatus.RUNNING && existing.workerId == workerId) {
                        existing.copyEntity(
                            status = SimulationJobStatus.FAILED,
                            errorMessage = errorMessage,
                            leaseExpiresAt = null,
                            workerId = null,
                            finishedAt = finishedAt,
                            updatedAt = OffsetDateTime.now(clock),
                        )
                    } else {
                        null
                    }
                }
            },
        )

    private fun updateById(
        id: UUID,
        updater: (SimulationJobEntity) -> SimulationJobEntity?,
    ): Long {
        val existing = jobs[id] ?: return 0L
        val updated = updater(existing) ?: return 0L
        jobs[id] = updated
        return 1L
    }

    private fun deleteWhere(predicate: (SimulationJobEntity) -> Boolean): Long {
        val idsToDelete = jobs.values.filter(predicate).mapNotNull { it.id }
        idsToDelete.forEach(jobs::remove)
        return idsToDelete.size.toLong()
    }

    private fun paginate(
        ordered: List<SimulationJobEntity>,
        pageable: Pageable,
    ): List<SimulationJobEntity> {
        if (!pageable.isPaged) {
            return ordered
        }

        val start = pageable.offset.toInt().coerceAtMost(ordered.size)
        val end = (start + pageable.pageSize).coerceAtMost(ordered.size)
        return if (start >= end) emptyList() else ordered.subList(start, end)
    }

    private fun parseUuid(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()

    private fun SimulationJobEntity.copyEntity(
        id: UUID? = this.id,
        ownerUserId: UUID? = this.ownerUserId,
        ownerGroupId: UUID? = this.ownerGroupId,
        requestedByUserId: UUID = this.requestedByUserId,
        type: com.ynixt.sharedfinances.domain.enums.SimulationJobType = this.type,
        status: SimulationJobStatus = this.status,
        requestPayload: String? = this.requestPayload,
        resultPayload: String? = this.resultPayload,
        errorMessage: String? = this.errorMessage,
        leaseExpiresAt: OffsetDateTime? = this.leaseExpiresAt,
        workerId: String? = this.workerId,
        startedAt: OffsetDateTime? = this.startedAt,
        finishedAt: OffsetDateTime? = this.finishedAt,
        cancelledAt: OffsetDateTime? = this.cancelledAt,
        retries: Int = this.retries,
        createdAt: OffsetDateTime? = this.createdAt,
        updatedAt: OffsetDateTime? = this.updatedAt,
    ): SimulationJobEntity =
        SimulationJobEntity(
            ownerUserId = ownerUserId,
            ownerGroupId = ownerGroupId,
            requestedByUserId = requestedByUserId,
            type = type,
            status = status,
            requestPayload = requestPayload,
            resultPayload = resultPayload,
            errorMessage = errorMessage,
            leaseExpiresAt = leaseExpiresAt,
            workerId = workerId,
            startedAt = startedAt,
            finishedAt = finishedAt,
            cancelledAt = cancelledAt,
            retries = retries,
        ).also { copied ->
            copied.id = id
            copied.createdAt = createdAt
            copied.updatedAt = updatedAt
        }

    private companion object {
        val pendingStatuses = setOf(SimulationJobStatus.QUEUED, SimulationJobStatus.RUNNING)

        val createdAtAscComparator =
            compareBy<SimulationJobEntity>({ it.createdAt }, { it.id?.toString() })

        val createdAtDescComparator = createdAtAscComparator.reversed()
    }
}

internal class InMemorySimulationJobDispatchQueueProducer : SimulationJobDispatchQueueProducer {
    private val queue = ArrayDeque<UUID>()

    override fun send(jobId: UUID) {
        queue.addLast(jobId)
    }

    fun poll(): UUID? =
        if (queue.isEmpty()) {
            null
        } else {
            queue.removeFirst()
        }
}

internal class NoOpActionEventService : ActionEventService {
    override fun getDestinationForUser(userId: UUID): String = "/topic/users/$userId/events"

    override fun getDestinationForGroup(userId: UUID): String = "/topic/groups/$userId/events"

    override suspend fun <T> newEvent(
        userId: UUID,
        type: ActionEventType,
        category: ActionEventCategory,
        data: T,
        groupInfo: NewEventGroupInfo?,
    ) {
    }
}

internal class ScenarioSimulationJobProcessor(
    private val groupService: GroupService,
) : SimulationJobProcessor {
    override suspend fun process(job: SimulationJobEntity): String {
        val groupId = job.ownerGroupId
        if (groupId == null) {
            return """{"outcomeBand":"FITS"}"""
        }

        val members = groupService.findAllMembers(job.requestedByUserId, groupId)
        val included = members.count { it.allowPlanningSimulator }
        val excluded = members.size - included
        val incompleteSimulation = excluded > 0

        return """
            {"outcomeBand":"FITS","groupContext":{"incompleteSimulation":$incompleteSimulation,"includedMembers":$included,"excludedMembers":$excluded}}
            """.trimIndent()
    }
}
