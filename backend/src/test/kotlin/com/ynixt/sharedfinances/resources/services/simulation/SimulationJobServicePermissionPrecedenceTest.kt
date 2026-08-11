package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.exceptions.http.PlanQuotaExceededException
import com.ynixt.sharedfinances.domain.exceptions.http.SimulationJobForbiddenException
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import com.ynixt.sharedfinances.domain.services.simulation.NewSimulationJobInput
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobProcessor
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.SimulationJobDispatchRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.SimulationJobRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SimulationJobServicePermissionPrecedenceTest {
    @Test
    fun `group permission refusal takes precedence over an exhausted personal quota`() =
        runTest {
            val requesterId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val permissions = Mockito.mock(GroupPermissionService::class.java)
            Mockito
                .`when`(
                    permissions.hasPermission(
                        requesterId,
                        groupId,
                        com.ynixt.sharedfinances.domain.enums.GroupPermissions.NEW_SIMULATION,
                    ),
                ).thenReturn(false)

            val service =
                SimulationJobServiceImpl(
                    simulationJobRepository = Mockito.mock(SimulationJobRepository::class.java),
                    simulationJobDatabaseClientRepository = Mockito.mock(SimulationJobDispatchRepository::class.java),
                    simulationJobDispatchQueueProducer =
                        Mockito.mock(com.ynixt.sharedfinances.domain.queue.producer.SimulationJobDispatchQueueProducer::class.java),
                    simulationJobProcessor = Mockito.mock(SimulationJobProcessor::class.java),
                    groupPermissionService = permissions,
                    actionEventService = Mockito.mock(ActionEventService::class.java),
                    planQuotaService = ExhaustedQuota,
                    clock = Clock.systemUTC(),
                )

            assertThrows<SimulationJobForbiddenException> {
                service.createForGroup(requesterId, groupId, NewSimulationJobInput())
            }
        }

    @Test
    fun `retention on day 31 preserves jobs counted in the current UTC month`() =
        runTest {
            val repository = Mockito.mock(SimulationJobRepository::class.java)
            val clock = Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC)
            val retentionThreshold = OffsetDateTime.parse("2026-08-01T12:00:00Z")
            val currentMonthStart = OffsetDateTime.parse("2026-08-01T00:00:00Z")
            Mockito
                .`when`(repository.deleteAllByCreatedAtBefore(retentionThreshold, currentMonthStart))
                .thenReturn(Mono.just(2))

            val service = service(repository, clock)

            kotlin.test.assertEquals(2, service.purgeOldJobs())
            Mockito.verify(repository).deleteAllByCreatedAtBefore(retentionThreshold, currentMonthStart)
        }

    private fun service(
        repository: SimulationJobRepository,
        clock: Clock,
    ) = SimulationJobServiceImpl(
        simulationJobRepository = repository,
        simulationJobDatabaseClientRepository = Mockito.mock(SimulationJobDispatchRepository::class.java),
        simulationJobDispatchQueueProducer =
            Mockito.mock(com.ynixt.sharedfinances.domain.queue.producer.SimulationJobDispatchQueueProducer::class.java),
        simulationJobProcessor = Mockito.mock(SimulationJobProcessor::class.java),
        groupPermissionService = Mockito.mock(GroupPermissionService::class.java),
        actionEventService = Mockito.mock(ActionEventService::class.java),
        planQuotaService = ExhaustedQuota,
        clock = clock,
    )

    private object ExhaustedQuota : PlanQuotaService {
        override suspend fun assertCanAdd(
            quotaOwnerUserId: UUID,
            quota: PlanLimitKey,
            requesterUserId: UUID,
        ): Nothing = throw PlanQuotaExceededException(quota)

        override suspend fun currentUsage(
            userId: UUID,
            quota: PlanLimitKey,
        ): Long = Long.MAX_VALUE

        override suspend fun usageChanged(
            userId: UUID,
            quota: PlanLimitKey,
        ) = Unit
    }
}
