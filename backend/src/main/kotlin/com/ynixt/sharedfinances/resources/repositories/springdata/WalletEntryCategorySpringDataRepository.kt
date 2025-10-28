package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntryCategorySpringDataRepository :
    WalletEntryCategoryRepository,
    Repository<WalletEntryCategory, String> {
    @Modifying
    @Query(
        """
        update wallet_entry_category
        set
            name = :newName,
            color = :newColor,
            parent_id = :newParentId
        where
            id = :id
            and user_id = :userId
    """,
    )
    override fun update(
        id: UUID,
        userId: UUID,
        newName: String,
        newColor: String,
        newParentId: UUID?,
    ): Mono<Long>
}
