package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.application.web.dto.events.GroupPlanUsageEventDto
import com.ynixt.sharedfinances.application.web.dto.events.PlanUsageEventDto
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.PlanQuotaExceededException
import com.ynixt.sharedfinances.domain.repositories.PlanQuotaUsageRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.plan.GroupPlanTierService
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Service
class PlanQuotaServiceImpl(
    private val userRepository: UserRepository,
    private val limitService: PlanLimitService,
    private val usageRepository: PlanQuotaUsageRepository,
    private val clock: Clock,
    private val actionEventService: ActionEventService,
    private val groupPlanTierService: GroupPlanTierService? = null,
) : PlanQuotaService {
    override suspend fun assertCanAdd(
        quotaOwnerUserId: UUID,
        quota: PlanLimitKey,
        requesterUserId: UUID,
    ) {
        require(quota.scope == com.ynixt.sharedfinances.domain.enums.PlanLimitScope.USER) {
            "Quota ${quota.name} is not user scoped"
        }
        val role = userRepository.findById(quotaOwnerUserId).awaitSingle().role
        if (role == UserPlanRole.ADMINISTRATOR) return
        usageRepository.acquireTransactionLock(quotaOwnerUserId, quota)
        val limit = limitService.resolve(role, quota)
        if (limit.unlimited) return
        val usage = currentUsage(quotaOwnerUserId, quota)
        if (usage >= requireNotNull(limit.value)) {
            throw PlanQuotaExceededException(
                quota = quota,
                quotaOwnerUserId = quotaOwnerUserId.takeIf { it != requesterUserId },
            )
        }
    }

    override suspend fun currentUsage(
        userId: UUID,
        quota: PlanLimitKey,
    ): Long = usageRepository.countUsage(userId, quota, utcMonthStart())

    override suspend fun usageChanged(
        userId: UUID,
        quota: PlanLimitKey,
    ) {
        actionEventService.newEvent(
            userId = userId,
            type = ActionEventType.UPDATE,
            category = ActionEventCategory.PLAN_USAGE,
            data = PlanUsageEventDto(quota, currentUsage(userId, quota)),
        )
    }

    override suspend fun assertGroupCanAdd(
        groupId: UUID,
        quota: PlanLimitKey,
        requesterUserId: UUID,
        includeOutstandingInvitations: Boolean,
    ) {
        require(quota.scope == com.ynixt.sharedfinances.domain.enums.PlanLimitScope.GROUP) {
            "Quota ${quota.name} is not group scoped"
        }
        usageRepository.acquireGroupTransactionLock(groupId, quota)
        val limit = limitService.resolve(requireNotNull(groupPlanTierService).resolve(groupId), quota)
        if (limit.unlimited) return
        val usage = currentGroupUsage(groupId, quota, includeOutstandingInvitations)
        if (usage >= requireNotNull(limit.value)) {
            throw PlanQuotaExceededException(quota = quota, groupId = groupId)
        }
    }

    override suspend fun currentGroupUsage(
        groupId: UUID,
        quota: PlanLimitKey,
        includeOutstandingInvitations: Boolean,
    ): Long = usageRepository.countGroupUsage(groupId, quota, includeOutstandingInvitations)

    override suspend fun groupUsageChanged(
        groupId: UUID,
        quota: PlanLimitKey,
        requesterUserId: UUID,
    ) {
        actionEventService.newEvent(
            userId = requesterUserId,
            type = ActionEventType.UPDATE,
            category = ActionEventCategory.GROUP_PLAN_USAGE,
            data = GroupPlanUsageEventDto(groupId, quota, currentGroupUsage(groupId, quota)),
            groupInfo = NewEventGroupInfo(groupId),
        )
    }

    private fun utcMonthStart() =
        clock
            .instant()
            .atZone(ZoneOffset.UTC)
            .with(TemporalAdjusters.firstDayOfMonth())
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
}
