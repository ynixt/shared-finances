package com.ynixt.sharedfinances.domain.models.plan

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import java.time.Instant

data class PlanQuotaEntitlement(
    val quota: PlanLimitKey,
    val limit: Int?,
    val usage: Long,
    val unlimited: Boolean,
    val windowEnd: Instant?,
)

data class UserEntitlements(
    val limitsEnabled: Boolean,
    val role: UserPlanRole,
    val importMaxLines: Int?,
    val quotas: List<PlanQuotaEntitlement>,
    val projectedDeletionAt: Instant? = null,
)
