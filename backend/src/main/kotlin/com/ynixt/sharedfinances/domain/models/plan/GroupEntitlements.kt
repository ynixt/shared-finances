package com.ynixt.sharedfinances.domain.models.plan

import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey

data class GroupQuotaEntitlement(
    val quota: PlanLimitKey,
    val limit: Int?,
    val usage: Long,
    val unlimited: Boolean,
)

data class GroupEntitlements(
    val limitsEnabled: Boolean,
    val tier: GroupPlanTier,
    val quotas: List<GroupQuotaEntitlement>,
)
