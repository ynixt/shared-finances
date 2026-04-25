package com.ynixt.sharedfinances.resources.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletCategoryConceptEntity
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptKind
import com.ynixt.sharedfinances.domain.exceptions.http.CategoryConceptNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidCategoryConceptSelectionException
import com.ynixt.sharedfinances.domain.repositories.WalletCategoryConceptRepository
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CategoryConceptServiceImpl(
    private val walletCategoryConceptRepository: WalletCategoryConceptRepository,
) : CategoryConceptService {
    override suspend fun findById(id: UUID): WalletCategoryConceptEntity? = walletCategoryConceptRepository.findById(id).awaitSingleOrNull()

    override suspend fun findRequiredByCode(code: WalletCategoryConceptCode): WalletCategoryConceptEntity =
        walletCategoryConceptRepository
            .findOneByCode(code)
            .awaitSingleOrNull()
            ?: throw IllegalStateException("Required predefined category concept $code was not found.")

    override suspend fun listAvailableForUser(userId: UUID): List<WalletCategoryConceptEntity> =
        walletCategoryConceptRepository
            .findAllAvailableForUser(userId)
            .collectList()
            .awaitSingle()

    override suspend fun resolveForMutation(
        conceptId: UUID?,
        customConceptName: String?,
    ): WalletCategoryConceptEntity {
        val normalizedCustomName = customConceptName?.trim()?.takeIf { it.isNotEmpty() }

        if (conceptId == null && normalizedCustomName == null) {
            throw InvalidCategoryConceptSelectionException("Either conceptId or customConceptName must be provided.")
        }

        if (conceptId != null && normalizedCustomName != null) {
            throw InvalidCategoryConceptSelectionException("conceptId and customConceptName cannot be provided together.")
        }

        if (conceptId != null) {
            return findById(conceptId) ?: throw CategoryConceptNotFoundException(conceptId)
        }

        return walletCategoryConceptRepository
            .save(
                WalletCategoryConceptEntity(
                    kind = WalletCategoryConceptKind.CUSTOM,
                    code = null,
                    displayName = normalizedCustomName,
                ),
            ).awaitSingle()
    }

    override suspend fun cleanupOrphanedCustomConcept(conceptId: UUID): Boolean =
        walletCategoryConceptRepository.deleteCustomIfOrphaned(conceptId).awaitSingle() > 0
}
