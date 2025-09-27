package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import com.ynixt.sharedfinances.domain.exceptions.DuplicatedCategoryException
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.UserCategoryService
import com.ynixt.sharedfinances.domain.services.actionevents.UserCategoryActionEventService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class UserCategoryServiceImpl(
    private val repository: WalletEntryCategoryRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val userCategoryActionEventService: UserCategoryActionEventService,
) : UserCategoryService {
    override fun findAllCategories(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletEntryCategory>> =
        createPage(pageable, countFn = { repository.countByUserId(userId) }) {
            repository.findAllByUserId(
                userId,
                pageable,
            )
        }

    override fun findCategory(
        userId: UUID,
        id: UUID,
    ): Mono<WalletEntryCategory> =
        repository.findOneByIdAndUserId(
            id = id,
            userId = userId,
        )

    override fun newCategory(
        userId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): Mono<WalletEntryCategory> =
        repository
            .save(
                WalletEntryCategory(
                    userId = userId,
                    name = newCategoryRequest.name,
                    color = newCategoryRequest.color,
                    groupId = null,
                    parentId = newCategoryRequest.parentId,
                ),
            ).flatMap { saved ->
                userCategoryActionEventService
                    .sendInsertedCategory(
                        category = saved,
                        userId = userId,
                    ).thenReturn(saved)
            }.onErrorMap { t ->
                if (databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_user_id_name")) {
                    DuplicatedCategoryException(
                        userId = userId,
                        groupId = null,
                        cause = t,
                    )
                } else {
                    t
                }
            }

    override fun editCategory(
        userId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): Mono<WalletEntryCategory> =
        repository
            .update(
                id = id,
                userId = userId,
                newName = editCategory.name,
                newColor = editCategory.color,
                newParentId = editCategory.parentId,
            ).flatMap { saved ->
                userCategoryActionEventService
                    .sendUpdatedCategory(
                        category = saved,
                        userId = userId,
                    ).thenReturn(saved)
            }

    override fun deleteCategory(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean> =
        repository
            .deleteByIdAndUserId(
                id = id,
                userId = userId,
            ).map { it > 0 }
}
