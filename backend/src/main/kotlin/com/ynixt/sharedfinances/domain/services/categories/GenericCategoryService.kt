package com.ynixt.sharedfinances.domain.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface GenericCategoryService {
    fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletEntryCategoryEntity>
}
