package com.ynixt.sharedfinances.resources.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import kotlinx.coroutines.reactor.awaitSingle
import java.util.UUID

abstract class CategoryService(
    protected val repository: WalletEntryCategoryRepository,
) {
    protected suspend fun mountChildren(categories: List<WalletEntryCategoryEntity>): List<WalletEntryCategoryEntity> =
        repository
            .findAllByParentIdIn(categories.map { it.id!! })
            .collectList()
            .awaitSingle()
            .let { children ->
                val byParentId: Map<UUID?, List<WalletEntryCategoryEntity>> =
                    children.groupBy { it.parentId }

                categories.onEach { parent ->
                    parent.children = byParentId[parent.id]
                }
            }
}
