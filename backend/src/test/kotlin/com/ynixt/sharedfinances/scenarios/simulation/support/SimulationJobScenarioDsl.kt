package com.ynixt.sharedfinances.scenarios.simulation.support

import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.SimulationJobStatus
import com.ynixt.sharedfinances.domain.enums.SimulationJobType
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.exceptions.http.SimulationJobNotFoundException
import com.ynixt.sharedfinances.domain.services.simulation.NewSimulationJobInput
import com.ynixt.sharedfinances.resources.services.simulation.SimulationJobServiceImpl
import com.ynixt.sharedfinances.scenarios.support.MutableScenarioClock
import com.ynixt.sharedfinances.scenarios.support.ScenarioGroupPermissionService
import com.ynixt.sharedfinances.scenarios.support.ScenarioGroupService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun simulationJobScenario(
    initialDate: LocalDate = LocalDate.of(2026, 1, 1),
    block: suspend SimulationJobScenarioDsl.() -> Unit,
): SimulationJobScenarioDsl =
    runBlocking {
        SimulationJobScenarioDsl(initialDate = initialDate).apply { block() }
    }

class SimulationJobScenarioDsl(
    initialDate: LocalDate = LocalDate.of(2026, 1, 1),
) {
    private val groupService = ScenarioGroupService()
    private val groupPermissionService = ScenarioGroupPermissionService(groupService)
    private val clock = MutableScenarioClock(initialDate)
    private val simulationJobStore = InMemorySimulationJobStore(clock)
    private val queueProducer = InMemorySimulationJobDispatchQueueProducer()
    private val simulationJobProcessor = ScenarioSimulationJobProcessor(groupService)

    private val simulationJobService =
        SimulationJobServiceImpl(
            simulationJobRepository = simulationJobStore,
            simulationJobDatabaseClientRepository = simulationJobStore,
            simulationJobDispatchQueueProducer = queueProducer,
            simulationJobProcessor = simulationJobProcessor,
            groupPermissionService = groupPermissionService,
            actionEventService = NoOpActionEventService(),
            clock = clock,
        )

    val given = Given(this)
    val whenActions = When(this)
    val then = Then(this)

    suspend fun given(block: suspend Given.() -> Unit): SimulationJobScenarioDsl =
        chain {
            given.block()
        }

    suspend fun `when`(block: suspend When.() -> Unit): SimulationJobScenarioDsl =
        chain {
            whenActions.block()
        }

    suspend fun then(block: suspend Then.() -> Unit): SimulationJobScenarioDsl =
        chain {
            then.block()
        }

    private suspend fun chain(action: suspend () -> Unit): SimulationJobScenarioDsl {
        action()
        return this
    }

    class Given internal constructor(
        private val dsl: SimulationJobScenarioDsl,
    ) {
        fun user(id: UUID = UUID.randomUUID()): UUID = id

        fun group(
            name: String = "Scenario Group",
            id: UUID = UUID.randomUUID(),
        ): UUID = dsl.groupService.createGroup(name = name, id = id)

        fun groupMember(
            groupId: UUID,
            userId: UUID,
            role: UserGroupRole = UserGroupRole.ADMIN,
            permissions: Set<GroupPermissions> = GroupPermissions.entries.toSet(),
            allowPlanningSimulator: Boolean = true,
        ) {
            dsl.groupService.upsertMemberScope(
                groupId = groupId,
                userId = userId,
                role = role,
                permissions = permissions,
                allowPlanningSimulator = allowPlanningSimulator,
            )
        }

        suspend fun memberPlanningSimulatorOptIn(
            groupId: UUID,
            userId: UUID,
            allowPlanningSimulator: Boolean,
        ) {
            val updated =
                dsl.groupService.updateOwnPlanningSimulatorOptIn(
                    userId = userId,
                    id = groupId,
                    allowPlanningSimulator = allowPlanningSimulator,
                )
            assertThat(updated).isTrue()
        }

        suspend fun staleRunningPersonalJob(
            ownerUserId: UUID,
            requestedByUserId: UUID = ownerUserId,
            type: SimulationJobType = SimulationJobType.PLANNING_SIMULATION,
            requestPayload: String? = """{"scenario":"stale"}""",
            leaseExpiredMinutesAgo: Long = 5,
            startedMinutesAgo: Long = 10,
        ): SimulationJobEntity {
            val now = OffsetDateTime.now(dsl.clock)
            return dsl.simulationJobStore
                .save(
                    SimulationJobEntity(
                        ownerUserId = ownerUserId,
                        ownerGroupId = null,
                        requestedByUserId = requestedByUserId,
                        type = type,
                        status = SimulationJobStatus.RUNNING,
                        requestPayload = requestPayload,
                        resultPayload = null,
                        errorMessage = null,
                        leaseExpiresAt = now.minusMinutes(leaseExpiredMinutesAgo),
                        workerId = "stale-worker",
                        startedAt = now.minusMinutes(startedMinutesAgo),
                        finishedAt = null,
                        cancelledAt = null,
                        retries = 0,
                    ),
                ).awaitSingle()
        }
    }

    class When internal constructor(
        private val dsl: SimulationJobScenarioDsl,
    ) {
        suspend fun createPersonalJob(
            ownerUserId: UUID,
            payload: String?,
        ): SimulationJobEntity =
            dsl.simulationJobService.create(
                ownerUserId = ownerUserId,
                input = NewSimulationJobInput(requestPayload = payload),
            )

        suspend fun createGroupJob(
            requesterUserId: UUID,
            groupId: UUID,
            payload: String?,
        ): SimulationJobEntity =
            dsl.simulationJobService.createForGroup(
                requesterUserId = requesterUserId,
                groupId = groupId,
                input = NewSimulationJobInput(requestPayload = payload),
            )

        fun enqueueDispatch(jobId: UUID) {
            dsl.queueProducer.send(jobId)
        }

        suspend fun processAllDispatches(maxIterations: Int = 50) {
            repeat(maxIterations) {
                val next = dsl.queueProducer.poll() ?: return
                dsl.simulationJobService.processDispatchMessage(next)
            }
            error("Dispatch queue exceeded $maxIterations iterations")
        }

        suspend fun reconcileExpiredLeases(): Long = dsl.simulationJobService.reconcileExpiredLeases()

        suspend fun dispatchNextForOwner(ownerUserId: UUID) {
            dsl.simulationJobService.dispatchNextQueuedForOwner(ownerUserId)
        }

        suspend fun cancelPersonalJob(
            ownerUserId: UUID,
            jobId: UUID,
        ): SimulationJobEntity =
            dsl.simulationJobService.cancelForOwner(
                ownerUserId = ownerUserId,
                jobId = jobId,
            )

        suspend fun deletePersonalJob(
            ownerUserId: UUID,
            jobId: UUID,
        ) {
            dsl.simulationJobService.deleteForOwner(
                ownerUserId = ownerUserId,
                jobId = jobId,
            )
        }

        suspend fun deleteGroupJob(
            requesterUserId: UUID,
            groupId: UUID,
            jobId: UUID,
        ) {
            dsl.simulationJobService.deleteForGroup(
                requesterUserId = requesterUserId,
                groupId = groupId,
                jobId = jobId,
            )
        }

        suspend fun getPersonalJob(
            ownerUserId: UUID,
            jobId: UUID,
        ): SimulationJobEntity =
            dsl.simulationJobService.getForOwner(
                ownerUserId = ownerUserId,
                jobId = jobId,
            )

        suspend fun getGroupJob(
            requesterUserId: UUID,
            groupId: UUID,
            jobId: UUID,
        ): SimulationJobEntity =
            dsl.simulationJobService.getForGroup(
                requesterUserId = requesterUserId,
                groupId = groupId,
                jobId = jobId,
            )
    }

    class Then internal constructor(
        private val dsl: SimulationJobScenarioDsl,
    ) {
        suspend fun personalJobStatusShouldBe(
            ownerUserId: UUID,
            jobId: UUID,
            expected: SimulationJobStatus,
        ) {
            val actual = dsl.simulationJobService.getForOwner(ownerUserId = ownerUserId, jobId = jobId).status
            assertThat(actual).isEqualTo(expected)
        }

        suspend fun groupJobStatusShouldBe(
            requesterUserId: UUID,
            groupId: UUID,
            jobId: UUID,
            expected: SimulationJobStatus,
        ) {
            val actual = dsl.simulationJobService.getForGroup(requesterUserId = requesterUserId, groupId = groupId, jobId = jobId).status
            assertThat(actual).isEqualTo(expected)
        }

        suspend fun personalJobRetriesShouldBe(
            ownerUserId: UUID,
            jobId: UUID,
            expected: Int,
        ) {
            val actual = dsl.simulationJobService.getForOwner(ownerUserId = ownerUserId, jobId = jobId).retries
            assertThat(actual).isEqualTo(expected)
        }

        suspend fun groupJobRetriesShouldBe(
            requesterUserId: UUID,
            groupId: UUID,
            jobId: UUID,
            expected: Int,
        ) {
            val actual = dsl.simulationJobService.getForGroup(requesterUserId = requesterUserId, groupId = groupId, jobId = jobId).retries
            assertThat(actual).isEqualTo(expected)
        }

        suspend fun personalJobResultShouldContain(
            ownerUserId: UUID,
            jobId: UUID,
            fragment: String,
        ) {
            val payload =
                dsl.simulationJobService
                    .getForOwner(ownerUserId = ownerUserId, jobId = jobId)
                    .resultPayload
                    .orEmpty()
            assertThat(payload).contains(fragment)
        }

        suspend fun groupJobResultShouldContain(
            requesterUserId: UUID,
            groupId: UUID,
            jobId: UUID,
            fragment: String,
        ) {
            val payload =
                dsl.simulationJobService
                    .getForGroup(requesterUserId = requesterUserId, groupId = groupId, jobId = jobId)
                    .resultPayload
                    .orEmpty()
            assertThat(payload).contains(fragment)
        }

        suspend fun personalJobShouldNotExist(
            ownerUserId: UUID,
            jobId: UUID,
        ) {
            val exception =
                runCatching {
                    dsl.simulationJobService.getForOwner(
                        ownerUserId = ownerUserId,
                        jobId = jobId,
                    )
                }.exceptionOrNull()

            assertThat(exception).isInstanceOf(SimulationJobNotFoundException::class.java)
        }

        suspend fun groupJobShouldNotExist(
            requesterUserId: UUID,
            groupId: UUID,
            jobId: UUID,
        ) {
            val exception =
                runCatching {
                    dsl.simulationJobService.getForGroup(
                        requesterUserId = requesterUserId,
                        groupId = groupId,
                        jobId = jobId,
                    )
                }.exceptionOrNull()

            assertThat(exception).isInstanceOf(SimulationJobNotFoundException::class.java)
        }
    }
}
