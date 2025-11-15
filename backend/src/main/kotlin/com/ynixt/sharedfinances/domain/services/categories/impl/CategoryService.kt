package com.ynixt.sharedfinances.domain.services.categories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import reactor.core.publisher.Mono
import java.util.UUID

abstract class CategoryService(
    protected val repository: WalletEntryCategoryRepository,
) {
    protected fun mountChildren(categories: Mono<List<WalletEntryCategoryEntity>>): Mono<List<WalletEntryCategoryEntity>> =
        categories.flatMap { categoriesList ->
            repository
                .findAllByParentIdIn(categoriesList.map { it.id!! })
                .collectList()
                .map { children ->
                    val byParentId: Map<UUID?, List<WalletEntryCategoryEntity>> =
                        children.groupBy { it.parentId }

                    categoriesList.onEach { parent ->
                        parent.children = byParentId[parent.id]
                    }
                }
        }
}
