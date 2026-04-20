package com.ynixt.sharedfinances.resources.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.exceptions.http.DebtSfCategoryProtectedException
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryConceptException
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryException
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
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
    categoryConceptService: CategoryConceptService,
    private val databaseHelperService: DatabaseHelperService,
    private val groupCategoryActionEventService: GroupCategoryActionEventService,
    private val groupPermissionService: GroupPermissionService,
) : CategoryService(
        repository = repository,
        categoryConceptService = categoryConceptService,
    ),
    GroupCategoryService {
    override suspend fun newCategories(
        groupId: UUID,
        categories: List<NewCategoryRequest>,
    ): List<WalletEntryCategoryEntity> {
        val persisted = mutableListOf<WalletEntryCategoryEntity>()

        categories.forEach { request ->
            val concept =
                categoryConceptService.resolveForMutation(
                    conceptId = request.conceptId,
                    customConceptName = request.customConceptName,
                )
            val conceptId = concept.id!!

            if (persisted.any { it.conceptId == conceptId } || hasConceptBoundToGroup(groupId = groupId, conceptId = conceptId)) {
                throw DuplicatedCategoryConceptException(
                    userId = null,
                    groupId = groupId,
                    conceptId = conceptId,
                )
            }

            persisted +=
                saveGroupCategory(
                    groupId = groupId,
                    name = request.name,
                    color = request.color,
                    parentId = request.parentId,
                    conceptId = conceptId,
                )
        }

        return persisted
    }

    override suspend fun ensureDebtSfCategory(groupId: UUID): WalletEntryCategoryEntity {
        val debtConceptId = debtSfConceptId()
        val existing =
            repository
                .findAllByGroupIdAndConceptId(
                    groupId = groupId,
                    conceptId = debtConceptId,
                ).collectList()
                .awaitSingle()

        if (existing.size == 1) {
            return existing.first()
        }
        if (existing.size > 1) {
            throw IllegalStateException("Group $groupId has ${existing.size} categories bound to DEBT_SF.")
        }

        return try {
            saveGroupCategory(
                groupId = groupId,
                name = DEBT_SF_DEFAULT_NAME,
                color = DEBT_SF_DEFAULT_COLOR,
                parentId = null,
                conceptId = debtConceptId,
            )
        } catch (t: Throwable) {
            if (databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_group_id_concept_id")) {
                repository
                    .findAllByGroupIdAndConceptId(
                        groupId = groupId,
                        conceptId = debtConceptId,
                    ).collectList()
                    .awaitSingle()
                    .singleOrNull()
                    ?: throw IllegalStateException("Unable to resolve DEBT_SF category after unique violation for group $groupId.", t)
            } else {
                throw t
            }
        }
    }

    override suspend fun findAllCategories(
        userId: UUID,
        groupId: UUID,
        onlyRoot: Boolean,
        mountChildren: Boolean,
        query: String?,
        pageable: Pageable,
    ): Page<WalletEntryCategoryEntity> {
        if (!groupPermissionService.hasPermission(userId = userId, groupId = groupId)) {
            return Page.empty()
        }

        return createPage(pageable, countFn = { repository.countByGroupId(groupId) }) {
            categoriesFluxForPage(
                groupId = groupId,
                onlyRoot = onlyRoot,
                query = query,
                pageable = pageable,
                shouldMountChildren = mountChildren,
            )
        }
    }

    private fun categoriesFluxForPage(
        groupId: UUID,
        onlyRoot: Boolean,
        query: String?,
        pageable: Pageable,
        shouldMountChildren: Boolean,
    ) = baseCategoriesFlux(groupId, onlyRoot, query, pageable).let { items ->
        if (shouldMountChildren) {
            mono { mountChildren(items.collectList().awaitSingle()) }.flatMapIterable { it }
        } else {
            items
        }
    }

    private fun baseCategoriesFlux(
        groupId: UUID,
        onlyRoot: Boolean,
        query: String?,
        pageable: Pageable,
    ) = when {
        onlyRoot && query == null ->
            repository.findAllByGroupIdAndParentIdIsNull(groupId, pageable)
        onlyRoot ->
            repository.findAllByGroupIdAndParentIdIsNullAndNameStartsWith(
                groupId,
                pageable,
                name = query!!,
            )
        query == null ->
            repository.findAllByGroupId(groupId, pageable)
        else ->
            repository.findAllByGroupIdAndNameStartsWith(
                groupId,
                pageable,
                name = query,
            )
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
                    val concept =
                        categoryConceptService.resolveForMutation(
                            conceptId = newCategoryRequest.conceptId,
                            customConceptName = newCategoryRequest.customConceptName,
                        )
                    val conceptId = concept.id!!

                    if (hasConceptBoundToGroup(groupId = groupId, conceptId = conceptId)) {
                        throw DuplicatedCategoryConceptException(
                            userId = null,
                            groupId = groupId,
                            conceptId = conceptId,
                        )
                    }

                    saveGroupCategory(
                        groupId = groupId,
                        name = newCategoryRequest.name,
                        color = newCategoryRequest.color,
                        parentId = newCategoryRequest.parentId,
                        conceptId = conceptId,
                    ).also { saved ->
                        groupCategoryActionEventService
                            .sendInsertedCategory(
                                category = saved,
                                userId = userId,
                            )
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
                    val existing =
                        repository
                            .findOneByIdAndGroupId(
                                id = id,
                                groupId = groupId,
                            ).awaitSingleOrNull()
                            ?: return@let null

                    val conceptToPersistId =
                        if (editCategory.conceptId != null || !editCategory.customConceptName.isNullOrBlank()) {
                            categoryConceptService
                                .resolveForMutation(
                                    conceptId = editCategory.conceptId,
                                    customConceptName = editCategory.customConceptName,
                                ).id!!
                        } else {
                            existing.conceptId
                        }

                    val existingIsDebtSf = isDebtSfConcept(existing.conceptId)
                    val targetIsDebtSf = isDebtSfConcept(conceptToPersistId)

                    if ((existingIsDebtSf && conceptToPersistId != existing.conceptId) || (!existingIsDebtSf && targetIsDebtSf)) {
                        throw DebtSfCategoryProtectedException(existing.id!!)
                    }

                    if (
                        conceptToPersistId != existing.conceptId &&
                        hasConceptBoundToGroup(
                            groupId = groupId,
                            conceptId = conceptToPersistId,
                            excludedCategoryId = id,
                        )
                    ) {
                        throw DuplicatedCategoryConceptException(
                            userId = null,
                            groupId = groupId,
                            conceptId = conceptToPersistId,
                        )
                    }

                    try {
                        val modifiedLines =
                            repository
                                .updateByGroupId(
                                    id = id,
                                    groupId = groupId,
                                    newName = editCategory.name,
                                    newColor = editCategory.color,
                                    newParentId = editCategory.parentId,
                                    newConceptId = conceptToPersistId,
                                ).awaitSingle()

                        if (modifiedLines <= 0) {
                            return@let null
                        }

                        val saved = findCategory(userId = userId, id = id, groupId = groupId, mountChildren = false)

                        if (saved != null) {
                            groupCategoryActionEventService
                                .sendUpdatedCategory(
                                    category = saved,
                                    userId = userId,
                                )
                        }

                        if (conceptToPersistId != existing.conceptId) {
                            categoryConceptService.cleanupOrphanedCustomConcept(existing.conceptId)
                        }

                        saved
                    } catch (t: Throwable) {
                        if (conceptToPersistId != existing.conceptId) {
                            categoryConceptService.cleanupOrphanedCustomConcept(conceptToPersistId)
                        }
                        throw when {
                            databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_group_id_name") ->
                                DuplicatedCategoryException(
                                    userId = null,
                                    groupId = groupId,
                                    cause = t,
                                )
                            databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_group_id_concept_id") ->
                                DuplicatedCategoryConceptException(
                                    userId = null,
                                    groupId = groupId,
                                    conceptId = conceptToPersistId,
                                    cause = t,
                                )
                            else -> t
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
                    val category =
                        repository
                            .findOneByIdAndGroupId(
                                id = id,
                                groupId = groupId,
                            ).awaitSingleOrNull()
                            ?: return@let false

                    if (isDebtSfConcept(category.conceptId)) {
                        throw DebtSfCategoryProtectedException(id)
                    }

                    val deleted =
                        repository
                            .deleteByIdAndGroupId(
                                id = id,
                                groupId = groupId,
                            ).awaitSingle()
                            .let { it > 0 }

                    if (deleted) {
                        categoryConceptService.cleanupOrphanedCustomConcept(category.conceptId)
                    }

                    deleted
                } else {
                    false
                }
            }

    private suspend fun saveGroupCategory(
        groupId: UUID,
        name: String,
        color: String,
        parentId: UUID?,
        conceptId: UUID,
    ): WalletEntryCategoryEntity =
        try {
            repository
                .save(
                    WalletEntryCategoryEntity(
                        userId = null,
                        name = name,
                        color = color,
                        groupId = groupId,
                        parentId = parentId,
                        conceptId = conceptId,
                    ),
                ).awaitSingle()
        } catch (t: Throwable) {
            categoryConceptService.cleanupOrphanedCustomConcept(conceptId)
            throw when {
                databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_group_id_name") ->
                    DuplicatedCategoryException(
                        userId = null,
                        groupId = groupId,
                        cause = t,
                    )
                databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_group_id_concept_id") ->
                    DuplicatedCategoryConceptException(
                        userId = null,
                        groupId = groupId,
                        conceptId = conceptId,
                        cause = t,
                    )
                else -> t
            }
        }

    private suspend fun hasConceptBoundToGroup(
        groupId: UUID,
        conceptId: UUID,
        excludedCategoryId: UUID? = null,
    ): Boolean =
        repository
            .findAllByGroupIdAndConceptId(
                groupId = groupId,
                conceptId = conceptId,
            ).collectList()
            .awaitSingle()
            .any { it.id != excludedCategoryId }
}
