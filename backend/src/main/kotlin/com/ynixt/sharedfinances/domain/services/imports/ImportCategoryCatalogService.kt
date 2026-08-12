package com.ynixt.sharedfinances.domain.services.imports

import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import java.util.UUID

interface ImportCategoryCatalogService {
    suspend fun findAll(userId: UUID): List<WalletEntryCategoryEntity>

    suspend fun findAllMembers(userId: UUID): List<GroupUserEntity>
}
