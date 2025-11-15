package com.ynixt.sharedfinances.domain.services.categories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.exceptions.DuplicatedCategoryException
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.categories.GroupCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.UUID

@Service
class GroupCategoryServiceImpl(
    repository: WalletEntryCategoryRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val groupCategoryActionEventService: GroupCategoryActionEventService,
    private val groupPermissionService: GroupPermissionService,
) : CategoryService(repository),
    GroupCategoryService {
    override fun newCategories(
        groupId: UUID,
        categories: List<NewCategoryRequest>,
    ): Mono<List<WalletEntryCategoryEntity>> =
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

    override fun findAllCategories(
        userId: UUID,
        groupId: UUID,
        onlyRoot: Boolean,
        mountChildren: Boolean,
        query: String?,
        pageable: Pageable,
    ): Mono<Page<WalletEntryCategoryEntity>> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).flatMap { hasPermission ->
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
                            mountChildren(items.collectList()).flatMapIterable { it }
                        } else {
                            items
                        }
                    }
                } else {
                    Mono.empty()
                }
            }

    override fun findCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
        mountChildren: Boolean,
    ): Mono<WalletEntryCategoryEntity> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    repository
                        .findOneByIdAndGroupId(
                            id = id,
                            groupId = groupId,
                        ).flatMap {
                            if (mountChildren) {
                                mountChildren(listOf(it).toMono()).flatMap { list -> Mono.just(list[0]) }
                            } else {
                                Mono.just(it)
                            }
                        }
                } else {
                    Mono.empty()
                }
            }

    override fun newCategory(
        userId: UUID,
        groupId: UUID,
        newCategoryRequest: NewCategoryRequest,
    ): Mono<WalletEntryCategoryEntity> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.NEW_CATEGORY,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    repository
                        .save(
                            WalletEntryCategoryEntity(
                                userId = null,
                                name = newCategoryRequest.name,
                                color = newCategoryRequest.color,
                                groupId = groupId,
                                parentId = newCategoryRequest.parentId,
                            ),
                        ).flatMap { saved ->
                            groupCategoryActionEventService
                                .sendInsertedCategory(
                                    category = saved,
                                    userId = userId,
                                ).thenReturn(saved)
                        }.onErrorMap { t ->
                            if (databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_group_id_name")) {
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
                    Mono.empty()
                }
            }

    override fun editCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): Mono<WalletEntryCategoryEntity> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.EDIT_CATEGORY,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    repository
                        .updateByGroupId(
                            id = id,
                            groupId = groupId,
                            newName = editCategory.name,
                            newColor = editCategory.color,
                            newParentId = editCategory.parentId,
                        ).flatMap {
                            if (it > 0) {
                                findCategory(userId = userId, id = id, groupId = groupId, mountChildren = false).flatMap { saved ->
                                    groupCategoryActionEventService
                                        .sendUpdatedCategory(
                                            category = saved,
                                            userId = userId,
                                        ).thenReturn(saved)
                                }
                            } else {
                                Mono.empty()
                            }
                        }
                } else {
                    Mono.empty()
                }
            }

    override fun deleteCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
    ): Mono<Boolean> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.DELETE_CATEGORY,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    repository
                        .deleteByIdAndGroupId(
                            id = id,
                            groupId = groupId,
                        ).map { it > 0 }
                } else {
                    Mono.empty()
                }
            }
}
