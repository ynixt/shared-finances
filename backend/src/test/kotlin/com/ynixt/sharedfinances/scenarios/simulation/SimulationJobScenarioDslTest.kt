package com.ynixt.sharedfinances.scenarios.simulation

import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.SimulationJobStatus
import com.ynixt.sharedfinances.domain.exceptions.http.SimulationJobForbiddenException
import com.ynixt.sharedfinances.scenarios.simulation.support.simulationJobScenario
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class SimulationJobScenarioDslTest {
    @Test
    fun `should enqueue and complete simulation job`() {
        lateinit var ownerUserId: UUID
        lateinit var createdJob: SimulationJobEntity

        simulationJobScenario {
            given {
                ownerUserId = user()
            }

            `when` {
                createdJob = createPersonalJob(ownerUserId = ownerUserId, payload = """{"scenario":"smoke"}""")
            }

            then {
                personalJobStatusShouldBe(
                    ownerUserId = ownerUserId,
                    jobId = createdJob.id!!,
                    expected = SimulationJobStatus.QUEUED,
                )
            }

            `when` {
                processAllDispatches()
            }

            then {
                personalJobStatusShouldBe(
                    ownerUserId = ownerUserId,
                    jobId = createdJob.id!!,
                    expected = SimulationJobStatus.COMPLETED,
                )
                personalJobRetriesShouldBe(
                    ownerUserId = ownerUserId,
                    jobId = createdJob.id!!,
                    expected = 1,
                )
                personalJobResultShouldContain(
                    ownerUserId = ownerUserId,
                    jobId = createdJob.id!!,
                    fragment = "\"outcomeBand\"",
                )
            }
        }
    }

    @Test
    fun `should allow owner cancellation and reject non-owner`() {
        lateinit var ownerUserId: UUID
        lateinit var otherUserId: UUID
        lateinit var queuedJob: SimulationJobEntity

        simulationJobScenario {
            given {
                ownerUserId = user()
                otherUserId = user()
            }

            `when` {
                createPersonalJob(ownerUserId = ownerUserId, payload = """{"scenario":"first"}""")
                queuedJob = createPersonalJob(ownerUserId = ownerUserId, payload = """{"scenario":"second"}""")
            }

            `when` {
                assertThrowsSuspend<SimulationJobForbiddenException> {
                    cancelPersonalJob(
                        ownerUserId = otherUserId,
                        jobId = queuedJob.id!!,
                    )
                }
            }

            `when` {
                cancelPersonalJob(
                    ownerUserId = ownerUserId,
                    jobId = queuedJob.id!!,
                )
            }

            then {
                personalJobStatusShouldBe(
                    ownerUserId = ownerUserId,
                    jobId = queuedJob.id!!,
                    expected = SimulationJobStatus.CANCELLED,
                )
            }
        }
    }

    @Test
    fun `should process duplicate dispatch idempotently`() {
        lateinit var ownerUserId: UUID
        lateinit var createdJob: SimulationJobEntity

        simulationJobScenario {
            given {
                ownerUserId = user()
            }

            `when` {
                createdJob = createPersonalJob(ownerUserId = ownerUserId, payload = """{"scenario":"duplicate"}""")
                enqueueDispatch(createdJob.id!!)
                enqueueDispatch(createdJob.id!!)
                processAllDispatches()
            }

            then {
                personalJobStatusShouldBe(
                    ownerUserId = ownerUserId,
                    jobId = createdJob.id!!,
                    expected = SimulationJobStatus.COMPLETED,
                )
                personalJobRetriesShouldBe(
                    ownerUserId = ownerUserId,
                    jobId = createdJob.id!!,
                    expected = 1,
                )
            }
        }
    }

    @Test
    fun `should reconcile expired running lease`() {
        lateinit var ownerUserId: UUID
        lateinit var staleJob: SimulationJobEntity
        var reconciled = 0L

        simulationJobScenario {
            given {
                ownerUserId = user()
                staleJob = staleRunningPersonalJob(ownerUserId = ownerUserId)
            }

            `when` {
                reconciled = reconcileExpiredLeases()
                dispatchNextForOwner(ownerUserId)
                processAllDispatches()
            }

            then {
                assertThat(reconciled).isGreaterThanOrEqualTo(1L)
                personalJobStatusShouldBe(
                    ownerUserId = ownerUserId,
                    jobId = staleJob.id!!,
                    expected = SimulationJobStatus.COMPLETED,
                )
            }
        }
    }

    @Test
    fun `should create and process simulation job for group scope`() {
        lateinit var ownerUserId: UUID
        lateinit var outsiderUserId: UUID
        lateinit var groupId: UUID
        lateinit var createdJob: SimulationJobEntity

        simulationJobScenario {
            given {
                ownerUserId = user()
                outsiderUserId = user()
                groupId = group(name = "Ops planning")
                groupMember(groupId = groupId, userId = ownerUserId)
            }

            `when` {
                createdJob =
                    createGroupJob(
                        requesterUserId = ownerUserId,
                        groupId = groupId,
                        payload = """{"scenario":"group"}""",
                    )
            }

            then {
                groupJobStatusShouldBe(
                    requesterUserId = ownerUserId,
                    groupId = groupId,
                    jobId = createdJob.id!!,
                    expected = SimulationJobStatus.QUEUED,
                )
            }

            `when` {
                assertThrowsSuspend<SimulationJobForbiddenException> {
                    getGroupJob(
                        requesterUserId = outsiderUserId,
                        groupId = groupId,
                        jobId = createdJob.id!!,
                    )
                }
                processAllDispatches()
            }

            then {
                groupJobStatusShouldBe(
                    requesterUserId = ownerUserId,
                    groupId = groupId,
                    jobId = createdJob.id!!,
                    expected = SimulationJobStatus.COMPLETED,
                )
                groupJobRetriesShouldBe(
                    requesterUserId = ownerUserId,
                    groupId = groupId,
                    jobId = createdJob.id!!,
                    expected = 1,
                )
            }
        }
    }

    @Test
    fun `should delete personal simulation job and return 404 after`() {
        lateinit var ownerUserId: UUID
        lateinit var createdJob: SimulationJobEntity

        simulationJobScenario {
            given {
                ownerUserId = user()
            }

            `when` {
                createdJob = createPersonalJob(ownerUserId = ownerUserId, payload = """{"scenario":"delete-me"}""")
                processAllDispatches()
                deletePersonalJob(ownerUserId = ownerUserId, jobId = createdJob.id!!)
            }

            then {
                personalJobShouldNotExist(
                    ownerUserId = ownerUserId,
                    jobId = createdJob.id!!,
                )
            }
        }
    }

    @Test
    fun `should forbid deleting another users simulation job`() {
        lateinit var ownerUserId: UUID
        lateinit var otherUserId: UUID
        lateinit var createdJob: SimulationJobEntity

        simulationJobScenario {
            given {
                ownerUserId = user()
                otherUserId = user()
            }

            `when` {
                createdJob = createPersonalJob(ownerUserId = ownerUserId, payload = """{"scenario":"private"}""")
            }

            `when` {
                assertThrowsSuspend<SimulationJobForbiddenException> {
                    deletePersonalJob(
                        ownerUserId = otherUserId,
                        jobId = createdJob.id!!,
                    )
                }
            }
        }
    }

    @Test
    fun `should delete group simulation job when requester has permission`() {
        lateinit var ownerUserId: UUID
        lateinit var groupId: UUID
        lateinit var createdJob: SimulationJobEntity

        simulationJobScenario {
            given {
                ownerUserId = user()
                groupId = group(name = "Deletable sims")
                groupMember(
                    groupId = groupId,
                    userId = ownerUserId,
                    permissions = GroupPermissions.entries.toSet(),
                )
            }

            `when` {
                createdJob = createGroupJob(requesterUserId = ownerUserId, groupId = groupId, payload = """{"scenario":"group-del"}""")
                processAllDispatches()
                deleteGroupJob(
                    requesterUserId = ownerUserId,
                    groupId = groupId,
                    jobId = createdJob.id!!,
                )
            }

            then {
                groupJobShouldNotExist(
                    requesterUserId = ownerUserId,
                    groupId = groupId,
                    jobId = createdJob.id!!,
                )
            }
        }
    }

    @Test
    fun `should flag incomplete group simulation when requester opts out`() {
        lateinit var ownerUserId: UUID
        lateinit var groupId: UUID
        lateinit var createdJob: SimulationJobEntity

        simulationJobScenario {
            given {
                ownerUserId = user()
                groupId = group(name = "Planner group")
                groupMember(groupId = groupId, userId = ownerUserId, allowPlanningSimulator = true)
                memberPlanningSimulatorOptIn(
                    groupId = groupId,
                    userId = ownerUserId,
                    allowPlanningSimulator = false,
                )
            }

            `when` {
                createdJob = createGroupJob(requesterUserId = ownerUserId, groupId = groupId, payload = "{}")
                processAllDispatches()
            }

            then {
                groupJobStatusShouldBe(
                    requesterUserId = ownerUserId,
                    groupId = groupId,
                    jobId = createdJob.id!!,
                    expected = SimulationJobStatus.COMPLETED,
                )
                groupJobResultShouldContain(
                    requesterUserId = ownerUserId,
                    groupId = groupId,
                    jobId = createdJob.id!!,
                    fragment = "\"incompleteSimulation\":true",
                )
            }
        }
    }
}

private suspend inline fun <reified T : Throwable> assertThrowsSuspend(noinline block: suspend () -> Unit): T {
    val exception = runCatching { block() }.exceptionOrNull()
    assertThat(exception).isInstanceOf(T::class.java)
    @Suppress("UNCHECKED_CAST")
    return exception as T
}
