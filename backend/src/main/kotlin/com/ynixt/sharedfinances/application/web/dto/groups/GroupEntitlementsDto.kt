package com.ynixt.sharedfinances.application.web.dto.groups

import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.models.plan.GroupEntitlements

data class GroupQuotaEntitlementDto(
    val quota: PlanLimitKey,
    val limit: Int?,
    val usage: Long,
    val unlimited: Boolean,
)

data class GroupEntitlementsDto(
    val limitsEnabled: Boolean,
    val tier: GroupPlanTier,
    val quotas: List<GroupQuotaEntitlementDto>,
) {
    companion object {
        fun from(model: GroupEntitlements) =
            GroupEntitlementsDto(
                limitsEnabled = model.limitsEnabled,
                tier = model.tier,
                quotas = model.quotas.map { GroupQuotaEntitlementDto(it.quota, it.limit, it.usage, it.unlimited) },
            )
    }
}
