package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import com.ynixt.sharedfinances.domain.repositories.BankAccountRepository
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface BankAccountSpringDataRepository :
    BankAccountRepository,
    ReactiveCrudRepository<BankAccount, String> {
    @Query(
        """
            update bank_account
            set name = :newName,
                enabled = :newEnabled,
                updated_at = CURRENT_TIMESTAMP
            where
                id = :id
                and user_id = :userId
        """,
    )
    override fun update(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
    ): Mono<Long>
}
