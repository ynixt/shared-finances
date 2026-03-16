package com.ynixt.sharedfinances.resources.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryException
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.UserCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.categories.UserCategoryService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserCategoryServiceImpl(
    repository: WalletEntryCategoryRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val userCategoryActionEventService: UserCategoryActionEventService,
) : CategoryService(repository),
    UserCategoryService {
    override suspend fun findAllCategories(
        userId: UUID,
        onlyRoot: Boolean,
        mountChildren: Boolean,
        query: String?,
        pageable: Pageable,
    ): Page<WalletEntryCategoryEntity> =
        createPage(pageable, countFn = { repository.countByUserId(userId) }) {
            val items =
                if (onlyRoot) {
                    if (query == null) {
                        repository.findAllByUserIdAndParentIdIsNull(
                            userId,
                            pageable,
                        )
                    } else {
                        repository.findAllByUserIdAndParentIdIsNullAndNameStartsWith(
                            userId,
                            pageable,
                            name = query,
                        )
                    }
                } else {
                    if (query == null) {
                        repository.findAllByUserId(
                            userId,
                            pageable,
                        )
                    } else {
                        repository.findAllByUserIdAndNameStartsWith(
                            userId,
                            pageable,
                            name = query,
                        )
                    }
                }

            if (mountChildren) {
                mono { mountChildren(items.collectList().awaitSingle()) }.flatMapIterable { it }
            } else {
                items
            }
        }

    override suspend fun findCategory(
        userId: UUID,
        id: UUID,
        mountChildren: Boolean,
    ): WalletEntryCategoryEntity? =
        repository
            .findOneByIdAndUserId(
                id = id,
                userId = userId,
            ).awaitSingleOrNull()
            ?.let {
                if (mountChildren) {
                    mountChildren(listOf(it)).firstOrNull()
                } else {
                    it
                }
            }

    override suspend fun newCategory(
        userId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): WalletEntryCategoryEntity =
        try {
            repository
                .save(
                    WalletEntryCategoryEntity(
                        userId = userId,
                        name = newCategoryRequest.name,
                        color = newCategoryRequest.color,
                        groupId = null,
                        parentId = newCategoryRequest.parentId,
                    ),
                ).awaitSingle()
                .also { saved ->
                    userCategoryActionEventService
                        .sendInsertedCategory(
                            category = saved,
                            userId = userId,
                        )
                }
        } catch (t: Throwable) {
            throw if (databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_user_id_name")) {
                DuplicatedCategoryException(
                    userId = userId,
                    groupId = null,
                    cause = t,
                )
            } else {
                t
            }
        }

    override suspend fun editCategory(
        userId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): WalletEntryCategoryEntity? =
        repository
            .updateByUserId(
                id = id,
                userId = userId,
                newName = editCategory.name,
                newColor = editCategory.color,
                newParentId = editCategory.parentId,
            ).awaitSingle()
            .let { modifiedLines ->
                if (modifiedLines > 0) {
                    findCategory(userId = userId, id = id, mountChildren = false)?.also { saved ->
                        userCategoryActionEventService
                            .sendUpdatedCategory(
                                category = saved,
                                userId = userId,
                            )
                    }
                } else {
                    null
                }
            }

    override suspend fun deleteCategory(
        userId: UUID,
        id: UUID,
    ): Boolean =
        repository
            .deleteByIdAndUserId(
                id = id,
                userId = userId,
            ).awaitSingle()
            .let { it > 0 }
}
