package com.ynixt.sharedfinances.domain.services.categories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.exceptions.DuplicatedCategoryException
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.UserCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.categories.UserCategoryService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.UUID

@Service
class UserCategoryServiceImpl(
    repository: WalletEntryCategoryRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val userCategoryActionEventService: UserCategoryActionEventService,
) : CategoryService(repository),
    UserCategoryService {
    override fun findAllCategories(
        userId: UUID,
        onlyRoot: Boolean,
        mountChildren: Boolean,
        query: String?,
        pageable: Pageable,
    ): Mono<Page<WalletEntryCategoryEntity>> =
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
                mountChildren(items.collectList()).flatMapIterable { it }
            } else {
                items
            }
        }

    override fun findCategory(
        userId: UUID,
        id: UUID,
        mountChildren: Boolean,
    ): Mono<WalletEntryCategoryEntity> =
        repository
            .findOneByIdAndUserId(
                id = id,
                userId = userId,
            ).flatMap {
                if (mountChildren) {
                    mountChildren(listOf(it).toMono()).flatMap { list -> Mono.just(list[0]) }
                } else {
                    Mono.just(it)
                }
            }

    override fun newCategory(
        userId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): Mono<WalletEntryCategoryEntity> =
        repository
            .save(
                WalletEntryCategoryEntity(
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
    ): Mono<WalletEntryCategoryEntity> =
        repository
            .updateByUserId(
                id = id,
                userId = userId,
                newName = editCategory.name,
                newColor = editCategory.color,
                newParentId = editCategory.parentId,
            ).flatMap {
                if (it > 0) {
                    findCategory(userId = userId, id = id, mountChildren = false).flatMap { saved ->
                        userCategoryActionEventService
                            .sendUpdatedCategory(
                                category = saved,
                                userId = userId,
                            ).thenReturn(saved)
                    }
                } else {
                    Mono.empty()
                }
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
