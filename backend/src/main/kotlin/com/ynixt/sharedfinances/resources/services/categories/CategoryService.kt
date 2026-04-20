package com.ynixt.sharedfinances.resources.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
import kotlinx.coroutines.reactor.awaitSingle
import java.util.UUID

abstract class CategoryService(
    protected val repository: WalletEntryCategoryRepository,
    protected val categoryConceptService: CategoryConceptService,
) {
    protected companion object {
        const val DEBT_SF_DEFAULT_NAME = "Debt SF"
        const val DEBT_SF_DEFAULT_COLOR = "#f31261"
    }

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

    protected suspend fun debtSfConceptId(): UUID = categoryConceptService.findRequiredByCode(WalletCategoryConceptCode.DEBT_SF).id!!

    protected suspend fun isDebtSfConcept(conceptId: UUID): Boolean = debtSfConceptId() == conceptId
}
