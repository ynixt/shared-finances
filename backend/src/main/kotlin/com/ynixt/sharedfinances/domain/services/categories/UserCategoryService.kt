package com.ynixt.sharedfinances.domain.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.util.UUID

interface UserCategoryService {
    fun findAllCategories(
        userId: UUID,
        onlyRoot: Boolean,
        mountChildren: Boolean,
        query: String?,
        pageable: Pageable,
    ): Mono<Page<WalletEntryCategoryEntity>>

    fun findCategory(
        userId: UUID,
        id: UUID,
        mountChildren: Boolean,
    ): Mono<WalletEntryCategoryEntity>

    fun newCategory(
        userId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): Mono<WalletEntryCategoryEntity>

    fun editCategory(
        userId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): Mono<WalletEntryCategoryEntity>

    fun deleteCategory(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean>
}
