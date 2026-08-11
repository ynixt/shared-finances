package com.ynixt.sharedfinances.application.web.dto.plan

import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole

data class PublishedPlanComparisonDto(
    val userPlans: List<PublishedUserPlanDto>,
    val groupTiers: List<PublishedGroupTierDto>,
)

data class PublishedUserPlanDto(
    val plan: UserPlanRole,
    val limits: List<PublishedPlanLimitDto>,
    val inactivityPolicy: PublishedInactivityPolicyDto,
)

data class PublishedGroupTierDto(
    val tier: GroupPlanTier,
    val limits: List<PublishedPlanLimitDto>,
)

data class PublishedPlanLimitDto(
    val quota: PlanLimitKey,
    val limit: Int?,
    val unlimited: Boolean,
)

data class PublishedInactivityPolicyDto(
    val retentionMonths: Int?,
    val unlimited: Boolean,
)
