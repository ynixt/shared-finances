package com.ynixt.sharedfinances.domain.services.plan

import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit

interface PlanLimitService {
    suspend fun resolve(
        plan: UserPlanRole,
        quota: PlanLimitKey,
    ): ResolvedPlanLimit

    suspend fun resolve(
        tier: GroupPlanTier,
        quota: PlanLimitKey,
    ): ResolvedPlanLimit = throw UnsupportedOperationException("Group limits are not supported")

    suspend fun save(limit: PlanLimitEntity): PlanLimitEntity

    suspend fun delete(
        scope: PlanLimitScope,
        plan: UserPlanRole,
        quota: PlanLimitKey,
    )
}
