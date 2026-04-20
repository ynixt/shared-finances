package com.ynixt.sharedfinances.resources.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.exceptions.http.DebtSfCategoryProtectedException
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryConceptException
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryException
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.UserCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
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
    categoryConceptService: CategoryConceptService,
    private val databaseHelperService: DatabaseHelperService,
    private val userCategoryActionEventService: UserCategoryActionEventService,
) : CategoryService(
        repository = repository,
        categoryConceptService = categoryConceptService,
    ),
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
    ): WalletEntryCategoryEntity {
        val concept =
            categoryConceptService.resolveForMutation(
                conceptId = newCategoryRequest.conceptId,
                customConceptName = newCategoryRequest.customConceptName,
            )
        val conceptId = concept.id!!

        if (hasConceptBoundToUser(userId = userId, conceptId = conceptId)) {
            throw DuplicatedCategoryConceptException(
                userId = userId,
                groupId = null,
                conceptId = conceptId,
            )
        }

        return try {
            repository
                .save(
                    WalletEntryCategoryEntity(
                        userId = userId,
                        name = newCategoryRequest.name,
                        color = newCategoryRequest.color,
                        groupId = null,
                        parentId = newCategoryRequest.parentId,
                        conceptId = conceptId,
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
            categoryConceptService.cleanupOrphanedCustomConcept(conceptId)
            throw when {
                databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_user_id_name") ->
                    DuplicatedCategoryException(
                        userId = userId,
                        groupId = null,
                        cause = t,
                    )
                databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_user_id_concept_id") ->
                    DuplicatedCategoryConceptException(
                        userId = userId,
                        groupId = null,
                        conceptId = conceptId,
                        cause = t,
                    )
                else -> t
            }
        }
    }

    override suspend fun ensureDebtSfCategory(userId: UUID): WalletEntryCategoryEntity {
        val debtConceptId = debtSfConceptId()
        val existing =
            repository
                .findAllByUserIdAndConceptId(
                    userId = userId,
                    conceptId = debtConceptId,
                ).collectList()
                .awaitSingle()

        if (existing.size == 1) {
            return existing.first()
        }
        if (existing.size > 1) {
            throw IllegalStateException("User $userId has ${existing.size} categories bound to DEBT_SF.")
        }

        return try {
            repository
                .save(
                    WalletEntryCategoryEntity(
                        name = DEBT_SF_DEFAULT_NAME,
                        color = DEBT_SF_DEFAULT_COLOR,
                        userId = userId,
                        groupId = null,
                        parentId = null,
                        conceptId = debtConceptId,
                    ),
                ).awaitSingle()
        } catch (t: Throwable) {
            if (databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_user_id_concept_id")) {
                repository
                    .findAllByUserIdAndConceptId(
                        userId = userId,
                        conceptId = debtConceptId,
                    ).collectList()
                    .awaitSingle()
                    .singleOrNull()
                    ?: throw IllegalStateException("Unable to resolve DEBT_SF category after unique violation for user $userId.", t)
            } else {
                throw t
            }
        }
    }

    override suspend fun editCategory(
        userId: UUID,
        id: UUID,
        editCategory: EditCategoryRequest,
    ): WalletEntryCategoryEntity? {
        val existing = findCategory(userId = userId, id = id, mountChildren = false) ?: return null
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

        if (conceptToPersistId != existing.conceptId &&
            hasConceptBoundToUser(userId = userId, conceptId = conceptToPersistId, excludedCategoryId = id)
        ) {
            throw DuplicatedCategoryConceptException(
                userId = userId,
                groupId = null,
                conceptId = conceptToPersistId,
            )
        }

        return try {
            val modifiedLines =
                repository
                    .updateByUserId(
                        id = id,
                        userId = userId,
                        newName = editCategory.name,
                        newColor = editCategory.color,
                        newParentId = editCategory.parentId,
                        newConceptId = conceptToPersistId,
                    ).awaitSingle()

            if (modifiedLines <= 0) {
                return null
            }

            val saved = findCategory(userId = userId, id = id, mountChildren = false)

            if (saved != null) {
                userCategoryActionEventService
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
                databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_user_id_name") ->
                    DuplicatedCategoryException(
                        userId = userId,
                        groupId = null,
                        cause = t,
                    )
                databaseHelperService.isUniqueViolation(t, "idx_wallet_entry_category_user_id_concept_id") ->
                    DuplicatedCategoryConceptException(
                        userId = userId,
                        groupId = null,
                        conceptId = conceptToPersistId,
                        cause = t,
                    )
                else -> t
            }
        }
    }

    override suspend fun deleteCategory(
        userId: UUID,
        id: UUID,
    ): Boolean {
        val existing = findCategory(userId = userId, id = id, mountChildren = false) ?: return false

        if (isDebtSfConcept(existing.conceptId)) {
            throw DebtSfCategoryProtectedException(id)
        }

        val deleted =
            repository
                .deleteByIdAndUserId(
                    id = id,
                    userId = userId,
                ).awaitSingle()
                .let { it > 0 }

        if (deleted) {
            categoryConceptService.cleanupOrphanedCustomConcept(existing.conceptId)
        }

        return deleted
    }

    private suspend fun hasConceptBoundToUser(
        userId: UUID,
        conceptId: UUID,
        excludedCategoryId: UUID? = null,
    ): Boolean =
        repository
            .findAllByUserIdAndConceptId(
                userId = userId,
                conceptId = conceptId,
            ).collectList()
            .awaitSingle()
            .any { it.id != excludedCategoryId }
}
