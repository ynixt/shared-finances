package com.ynixt.sharedfinances.domain.services.categories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryException
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.categories.GroupCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupCategoryServiceImpl(
    repository: WalletEntryCategoryRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val groupCategoryActionEventService: GroupCategoryActionEventService,
    private val groupPermissionService: GroupPermissionService,
) : CategoryService(repository),
    GroupCategoryService {
    override suspend fun newCategories(
        groupId: UUID,
        categories: List<NewCategoryRequest>,
    ): List<WalletEntryCategoryEntity> =
        repository
            .saveAll(
                categories.map {
                    WalletEntryCategoryEntity(
                        name = it.name,
                        parentId = it.parentId,
                        color = it.color,
                        groupId = groupId,
                        userId = null,
                    )
                },
            ).collectList()
            .awaitSingle()

    override suspend fun findAllCategories(
        userId: UUID,
        groupId: UUID,
        onlyRoot: Boolean,
        mountChildren: Boolean,
        query: String?,
        pageable: Pageable,
    ): Page<WalletEntryCategoryEntity> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).let { hasPermission ->
                if (hasPermission) {
                    createPage(pageable, countFn = { repository.countByGroupId(groupId) }) {
                        val items =
                            if (onlyRoot) {
                                if (query == null) {
                                    repository.findAllByGroupIdAndParentIdIsNull(
                                        groupId,
                                        pageable,
                                    )
                                } else {
                                    repository.findAllByGroupIdAndParentIdIsNullAndNameStartsWith(
                                        groupId,
                                        pageable,
                                        name = query,
                                    )
                                }
                            } else {
                                if (query == null) {
                                    repository.findAllByGroupId(
                                        groupId,
                                        pageable,
                                    )
                                } else {
                                    repository.findAllByGroupIdAndNameStartsWith(
                                        groupId,
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
                } else {
                    Page.empty()
                }
            }

    override suspend fun findCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
        mountChildren: Boolean,
    ): WalletEntryCategoryEntity? =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).let { hasPermission ->
                if (hasPermission) {
                    repository
                        .findOneByIdAndGroupId(
                            id = id,
                            groupId = groupId,
                        ).awaitSingleOrNull()
                        ?.let {
                            if (mountChildren) {
                                mountChildren(listOf(it)).firstOrNull()
                            } else {
                                it
                            }
                        }
                } else {
                    null
                }
            }

    override suspend fun newCategory(
        userId: UUID,
        groupId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): WalletEntryCategoryEntity? =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.NEW_CATEGORY,
            ).let { hasPermission ->
                if (hasPermission) {
                    try {
                        repository
                            .save(
                                WalletEntryCategoryEntity(
                                    userId = null,
                                    name = newCategoryRequest.name,
                                    color = newCategoryRequest.color,
                                    groupId = groupId,
                                    parentId = newCategoryRequest.parentId,
                                ),
                            ).awaitSingle()
                            .also { saved ->
                                groupCategoryActionEventService
                                    .sendInsertedCategory(
                                        category = saved,
                                        userId = userId,
                                    )
                            }
                    } catch (t: Throwable) {
                        throw if (databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_group_id_name")) {
                            DuplicatedCategoryException(
                                userId = null,
                                groupId = groupId,
                                cause = t,
                            )
                        } else {
                            t
                        }
                    }
                } else {
                    null
                }
            }

    override suspend fun editCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): WalletEntryCategoryEntity? =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.EDIT_CATEGORY,
            ).let { hasPermission ->
                if (hasPermission) {
                    repository
                        .updateByGroupId(
                            id = id,
                            groupId = groupId,
                            newName = editCategory.name,
                            newColor = editCategory.color,
                            newParentId = editCategory.parentId,
                        ).awaitSingle()
                        .let { modifiedLines ->
                            if (modifiedLines > 0) {
                                findCategory(userId = userId, id = id, groupId = groupId, mountChildren = false)?.also { saved ->
                                    groupCategoryActionEventService
                                        .sendUpdatedCategory(
                                            category = saved,
                                            userId = userId,
                                        )
                                }
                            } else {
                                null
                            }
                        }
                } else {
                    null
                }
            }

    override suspend fun deleteCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
    ): Boolean =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.DELETE_CATEGORY,
            ).let { hasPermission ->
                if (hasPermission) {
                    repository
                        .deleteByIdAndGroupId(
                            id = id,
                            groupId = groupId,
                        ).awaitSingle()
                        .let { it > 0 }
                } else {
                    false
                }
            }
}
