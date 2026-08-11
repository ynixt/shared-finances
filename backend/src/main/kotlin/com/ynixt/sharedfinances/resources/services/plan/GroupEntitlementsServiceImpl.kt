package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.models.plan.GroupEntitlements
import com.ynixt.sharedfinances.domain.models.plan.GroupQuotaEntitlement
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.plan.GroupEntitlementsService
import com.ynixt.sharedfinances.domain.services.plan.GroupPlanTierService
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupEntitlementsServiceImpl(
    private val groupPermissionService: GroupPermissionService,
    private val tierService: GroupPlanTierService,
    private val limitService: PlanLimitService,
    private val quotaService: PlanQuotaService,
    private val planProperties: PlanProperties,
) : GroupEntitlementsService {
    override suspend fun get(
        userId: UUID,
        groupId: UUID,
    ): GroupEntitlements? {
        if (!groupPermissionService.hasPermission(userId, groupId)) return null
        val tier = tierService.resolve(groupId)
        return GroupEntitlements(
            limitsEnabled = planProperties.enabled,
            tier = tier,
            quotas =
                PlanLimitKey.entries.filter { it.scope == PlanLimitScope.GROUP }.map { quota ->
                    val limit = limitService.resolve(tier, quota)
                    GroupQuotaEntitlement(
                        quota = quota,
                        limit = limit.value,
                        usage = quotaService.currentGroupUsage(groupId, quota),
                        unlimited = limit.unlimited,
                    )
                },
        )
    }
}
