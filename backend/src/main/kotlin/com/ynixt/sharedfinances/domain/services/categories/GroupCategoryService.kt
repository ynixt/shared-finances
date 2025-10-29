package com.ynixt.sharedfinances.domain.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupCategoryService {
    fun newCategories(
        groupId: UUID,
        categories: List<NewCategoryRequest>,
    ): Mono<List<WalletEntryCategory>>

    fun findAllCategories(
        userId: UUID,
        groupId: UUID,
        onlyRoot: Boolean,
        mountChildren: Boolean,
        query: String?,
        pageable: Pageable,
    ): Mono<Page<WalletEntryCategory>>

    fun findCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
        mountChildren: Boolean,
    ): Mono<WalletEntryCategory>

    fun newCategory(
        userId: UUID,
        groupId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): Mono<WalletEntryCategory>

    fun editCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): Mono<WalletEntryCategory>

    fun deleteCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
    ): Mono<Boolean>
}
