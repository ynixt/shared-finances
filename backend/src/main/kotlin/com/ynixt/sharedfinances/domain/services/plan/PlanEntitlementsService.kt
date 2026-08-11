package com.ynixt.sharedfinances.domain.services.plan

import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.plan.UserEntitlements
import java.time.OffsetDateTime
import java.util.UUID

interface PlanEntitlementsService {
    suspend fun get(
        userId: UUID,
        role: UserPlanRole,
        lastLoginAt: OffsetDateTime,
    ): UserEntitlements
}
