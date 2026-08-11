package com.ynixt.sharedfinances.domain.services.plan

import com.ynixt.sharedfinances.domain.models.plan.GroupEntitlements
import java.util.UUID

interface GroupEntitlementsService {
    suspend fun get(
        userId: UUID,
        groupId: UUID,
    ): GroupEntitlements?
}
