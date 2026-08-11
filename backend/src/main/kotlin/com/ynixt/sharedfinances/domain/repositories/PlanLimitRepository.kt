package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole

interface PlanLimitRepository {
    suspend fun findAll(): List<PlanLimitEntity>

    suspend fun upsert(limit: PlanLimitEntity): PlanLimitEntity

    suspend fun delete(
        scope: PlanLimitScope,
        plan: UserPlanRole,
        quota: PlanLimitKey,
    )
}
