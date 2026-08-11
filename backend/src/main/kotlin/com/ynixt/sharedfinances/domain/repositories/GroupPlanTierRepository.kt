package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import java.util.UUID

interface GroupPlanTierRepository {
    suspend fun resolve(groupId: UUID): GroupPlanTier?
}
