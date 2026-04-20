package com.ynixt.sharedfinances.domain.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletCategoryConceptEntity
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import java.util.UUID

interface CategoryConceptService {
    suspend fun findById(id: UUID): WalletCategoryConceptEntity?

    suspend fun findRequiredByCode(code: WalletCategoryConceptCode): WalletCategoryConceptEntity

    suspend fun listAvailableForUser(userId: UUID): List<WalletCategoryConceptEntity>

    suspend fun resolveForMutation(
        conceptId: UUID?,
        customConceptName: String?,
    ): WalletCategoryConceptEntity

    suspend fun cleanupOrphanedCustomConcept(conceptId: UUID): Boolean
}
