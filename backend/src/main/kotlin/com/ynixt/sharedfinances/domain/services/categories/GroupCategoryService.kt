package com.ynixt.sharedfinances.domain.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface GroupCategoryService {
    suspend fun newCategories(
        groupId: UUID,
        categories: List<NewCategoryRequest>,
    ): List<WalletEntryCategoryEntity>

    suspend fun findAllCategories(
        userId: UUID,
        groupId: UUID,
        onlyRoot: Boolean,
        mountChildren: Boolean,
        query: String?,
        pageable: Pageable,
    ): Page<WalletEntryCategoryEntity>

    suspend fun findCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
        mountChildren: Boolean,
    ): WalletEntryCategoryEntity?

    suspend fun newCategory(
        userId: UUID,
        groupId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): WalletEntryCategoryEntity?

    suspend fun editCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): WalletEntryCategoryEntity?

    suspend fun deleteCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
    ): Boolean
}
