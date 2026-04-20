package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntryCategorySpringDataRepository :
    WalletEntryCategoryRepository,
    R2dbcRepository<WalletEntryCategoryEntity, String> {
    @Modifying
    @Query(
        """
        update wallet_entry_category
        set
            name = :newName,
            color = :newColor,
            parent_id = :newParentId,
            concept_id = :newConceptId
        where
            id = :id
            and user_id = :userId
    """,
    )
    override fun updateByUserId(
        id: UUID,
        userId: UUID,
        newName: String,
        newColor: String,
        newParentId: UUID?,
        newConceptId: UUID,
    ): Mono<Long>

    @Modifying
    @Query(
        """
        update wallet_entry_category
        set
            name = :newName,
            color = :newColor,
            parent_id = :newParentId,
            concept_id = :newConceptId
        where
            id = :id
            and group_id = :groupId
    """,
    )
    override fun updateByGroupId(
        id: UUID,
        groupId: UUID,
        newName: String,
        newColor: String,
        newParentId: UUID?,
        newConceptId: UUID,
    ): Mono<Long>
}
