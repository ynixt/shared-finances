package com.ynixt.sharedfinances.application.web.dto.user

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import java.time.Instant

data class PlanQuotaEntitlementDto(
    val quota: PlanLimitKey,
    val limit: Int?,
    val usage: Long,
    val unlimited: Boolean,
    val windowEnd: Instant?,
)

data class UserEntitlementsDto(
    val limitsEnabled: Boolean,
    val role: UserPlanRole,
    val importMaxLines: Int?,
    val quotas: List<PlanQuotaEntitlementDto>,
    val projectedDeletionAt: Instant? = null,
)
