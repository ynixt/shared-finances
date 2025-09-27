package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface BankAccountSpringDataRepository : ReactiveCrudRepository<BankAccount, String> {
    fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Flux<BankAccount>

    fun countByUserId(userId: UUID): Mono<Long>

    fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<BankAccount>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long>

    @Modifying
    @Query(
        """
            update bank_account
            set name = :newName,
                enabled = :newEnabled,
                currency = :newCurrency,
                updated_at = CURRENT_TIMESTAMP
            where
                id = :id
                and user_id = :userId
        """,
    )
    fun update(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
    ): Mono<Long>
}
