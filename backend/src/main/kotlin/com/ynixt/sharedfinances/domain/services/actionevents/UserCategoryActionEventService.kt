package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import java.util.UUID

interface UserCategoryActionEventService {
    suspend fun sendInsertedCategory(
        userId: UUID,
        category: WalletEntryCategoryEntity,
    )

    suspend fun sendUpdatedCategory(
        userId: UUID,
        category: WalletEntryCategoryEntity,
    )

    suspend fun sendDeletedCategory(
        userId: UUID,
        id: UUID,
    )
}
