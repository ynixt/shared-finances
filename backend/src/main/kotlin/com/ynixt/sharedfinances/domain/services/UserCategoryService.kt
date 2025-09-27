package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.util.UUID

interface UserCategoryService {
    fun findAllCategories(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletEntryCategory>>

    fun findCategory(
        userId: UUID,
        id: UUID,
    ): Mono<WalletEntryCategory>

    fun newCategory(
        userId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): Mono<WalletEntryCategory>

    fun editCategory(
        userId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): Mono<WalletEntryCategory>

    fun deleteCategory(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean>
}
