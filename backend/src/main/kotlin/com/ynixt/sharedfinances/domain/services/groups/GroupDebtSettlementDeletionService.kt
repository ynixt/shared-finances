package com.ynixt.sharedfinances.domain.services.groups

import java.util.UUID

interface GroupDebtSettlementDeletionService {
    suspend fun deleteSettlement(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
    )
}
