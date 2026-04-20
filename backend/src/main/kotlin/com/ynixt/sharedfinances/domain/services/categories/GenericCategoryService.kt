package com.ynixt.sharedfinances.domain.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface GenericCategoryService {
    suspend fun findById(id: UUID): WalletEntryCategoryEntity?

    fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletEntryCategoryEntity>

    suspend fun findAllByGroupIdAndConceptId(
        groupId: UUID,
        conceptId: UUID,
    ): List<WalletEntryCategoryEntity>
}
