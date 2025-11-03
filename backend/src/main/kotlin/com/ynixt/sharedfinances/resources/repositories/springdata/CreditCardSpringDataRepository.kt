package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface CreditCardSpringDataRepository : ReactiveCrudRepository<CreditCard, String> {
    fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Flux<CreditCard>

    fun countByUserId(userId: UUID): Mono<Long>

    fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<CreditCard>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long>

    @Modifying
    @Query(
        """
            update credit_card
            set name = :newName,
                enabled = :newEnabled,
                currency = :newCurrency,
                available_limit = available_limit + (:newTotalLimit - total_limit),
                total_limit = :newTotalLimit,
                due_day = :newDueDay,
                days_between_due_and_closing = :newDaysBetweenDueAndClosing,
                due_on_next_business_day = :newDueOnNextBusinessDay,
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
        newTotalLimit: java.math.BigDecimal,
        newDueDay: Int,
        newDaysBetweenDueAndClosing: Int,
        newDueOnNextBusinessDay: Boolean,
    ): Mono<Long>
}
