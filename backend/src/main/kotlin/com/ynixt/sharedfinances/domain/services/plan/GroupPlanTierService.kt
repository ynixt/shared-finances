package com.ynixt.sharedfinances.domain.services.plan

import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import java.util.UUID

interface GroupPlanTierService {
    suspend fun resolve(groupId: UUID): GroupPlanTier
}
