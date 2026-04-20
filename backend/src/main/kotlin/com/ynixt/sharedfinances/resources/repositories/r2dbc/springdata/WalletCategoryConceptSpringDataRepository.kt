package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletCategoryConceptEntity
import com.ynixt.sharedfinances.domain.repositories.WalletCategoryConceptRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletCategoryConceptSpringDataRepository :
    WalletCategoryConceptRepository,
    R2dbcRepository<WalletCategoryConceptEntity, String> {
    @Query(
        """
        SELECT concept.*
        FROM wallet_category_concept concept
        WHERE concept.kind = 'PREDEFINED'
            OR EXISTS (
                SELECT 1
                FROM wallet_entry_category category
                WHERE category.concept_id = concept.id
                  AND (
                    category.user_id = :userId
                    OR category.group_id IN (
                        SELECT gu.group_id
                        FROM group_user gu
                        WHERE gu.user_id = :userId
                    )
                  )
            )
        ORDER BY
            CASE WHEN concept.kind = 'PREDEFINED' THEN 0 ELSE 1 END,
            concept.code ASC NULLS LAST,
            concept.display_name ASC NULLS LAST,
            concept.id ASC
        """,
    )
    override fun findAllAvailableForUser(userId: UUID): Flux<WalletCategoryConceptEntity>

    @Modifying
    @Query(
        """
        DELETE FROM wallet_category_concept concept
        WHERE concept.id = :id
          AND concept.kind = 'CUSTOM'
          AND NOT EXISTS (
              SELECT 1
              FROM wallet_entry_category category
              WHERE category.concept_id = concept.id
          )
        """,
    )
    override fun deleteCustomIfOrphaned(id: UUID): Mono<Long>
}
