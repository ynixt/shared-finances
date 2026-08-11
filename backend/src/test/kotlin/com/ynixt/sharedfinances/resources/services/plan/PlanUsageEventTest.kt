package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.application.web.dto.events.GroupPlanUsageEventDto
import com.ynixt.sharedfinances.application.web.dto.events.PlanUsageEventDto
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.repositories.PlanQuotaUsageRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class PlanUsageEventTest {
    @Test
    fun `group event carries one group quota and group facts only`() =
        runTest {
            val events = RecordingGroupEvents()
            val usage = FixedUsage(7)
            val service =
                PlanQuotaServiceImpl(
                    userRepository = Mockito.mock(UserRepository::class.java),
                    limitService = Mockito.mock(PlanLimitService::class.java),
                    usageRepository = usage,
                    clock = Clock.systemUTC(),
                    actionEventService = events,
                )
            val groupId = UUID.randomUUID()
            val actorId = UUID.randomUUID()

            assertEquals(7, service.currentGroupUsage(groupId, PlanLimitKey.GROUP_MEMBERS))
            assertEquals(0, events.calls)
            service.groupUsageChanged(groupId, PlanLimitKey.GROUP_MEMBERS, actorId)

            assertEquals(1, events.calls)
            assertEquals(
                GroupPlanUsageEventDto(groupId, PlanLimitKey.GROUP_MEMBERS, 7),
                events.data,
            )
            assertEquals(groupId, events.groupInfo?.groupId)
        }

    @Test
    fun `event contains only the affected quota and its current usage`() =
        runTest {
            val events = RecordingEvents()
            val usage = FixedUsage(4)
            val service =
                PlanQuotaServiceImpl(
                    userRepository = Mockito.mock(UserRepository::class.java),
                    limitService = Mockito.mock(PlanLimitService::class.java),
                    usageRepository = usage,
                    clock = Clock.systemUTC(),
                    actionEventService = events,
                )
            val userId = UUID.randomUUID()

            assertEquals(0, events.sent.size)
            service.usageChanged(userId, PlanLimitKey.GOALS)

            assertEquals(1, events.sent.size)
            val sent = events.sent.single()
            assertEquals(userId, sent.first)
            assertEquals(PlanUsageEventDto(PlanLimitKey.GOALS, 4), sent.second)

            usage.usage = 3
            service.usageChanged(userId, PlanLimitKey.GOALS)
            assertEquals(PlanUsageEventDto(PlanLimitKey.GOALS, 3), events.sent.last().second)
        }

    private class FixedUsage(
        var usage: Long,
    ) : PlanQuotaUsageRepository {
        override suspend fun acquireTransactionLock(
            userId: UUID,
            quota: PlanLimitKey,
        ) = Unit

        override suspend fun countUsage(
            userId: UUID,
            quota: PlanLimitKey,
            utcMonthStart: Instant,
        ): Long = usage

        override suspend fun countGroupUsage(
            groupId: UUID,
            quota: PlanLimitKey,
            includeOutstandingInvitations: Boolean,
        ): Long = usage
    }

    private class RecordingEvents : ActionEventService {
        val sent = mutableListOf<Pair<UUID, PlanUsageEventDto>>()

        override fun getDestinationForUser(userId: UUID): String = userId.toString()

        override fun getDestinationForGroup(userId: UUID): String = userId.toString()

        override suspend fun <T> newEvent(
            userId: UUID,
            type: ActionEventType,
            category: ActionEventCategory,
            data: T,
            groupInfo: NewEventGroupInfo?,
        ) {
            assertEquals(ActionEventType.UPDATE, type)
            assertEquals(ActionEventCategory.PLAN_USAGE, category)
            sent += userId to (data as PlanUsageEventDto)
        }
    }

    private class RecordingGroupEvents : ActionEventService {
        var calls = 0
        var data: GroupPlanUsageEventDto? = null
        var groupInfo: NewEventGroupInfo? = null

        override fun getDestinationForUser(userId: UUID) = userId.toString()

        override fun getDestinationForGroup(userId: UUID) = userId.toString()

        override suspend fun <T> newEvent(
            userId: UUID,
            type: ActionEventType,
            category: ActionEventCategory,
            data: T,
            groupInfo: NewEventGroupInfo?,
        ) {
            calls++
            assertEquals(ActionEventCategory.GROUP_PLAN_USAGE, category)
            this.data = data as GroupPlanUsageEventDto
            this.groupInfo = groupInfo
        }
    }
}
