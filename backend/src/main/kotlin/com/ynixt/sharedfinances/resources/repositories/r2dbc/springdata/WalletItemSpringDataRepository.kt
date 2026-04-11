package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

interface WalletItemSpringDataRepository :
    WalletItemRepository,
    R2dbcRepository<WalletItemEntity, String> {
    @Query("select distinct currency from wallet_item")
    override fun findDistinctCurrencies(): Flux<String>

    @Modifying
    @Query(
        """
            update wallet_item
            set name = :newName,
                enabled = :newEnabled,
                currency = :newCurrency,
                show_on_dashboard = :newShowOnDashboard,
                balance = balance + (:newTotalLimit - total_limit),
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
    override fun updateCreditCard(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
        newShowOnDashboard: Boolean,
        newTotalLimit: BigDecimal,
        newDueDay: Int,
        newDaysBetweenDueAndClosing: Int,
        newDueOnNextBusinessDay: Boolean,
    ): Mono<Long>

    @Modifying
    @Query(
        """
            update wallet_item
            set name = :newName,
                enabled = :newEnabled,
                currency = :newCurrency,
                show_on_dashboard = :newShowOnDashboard,
                updated_at = CURRENT_TIMESTAMP
            where
                id = :id
                and user_id = :userId
        """,
    )
    override fun updateBankAccount(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
        newShowOnDashboard: Boolean,
    ): Mono<Long>

    @Modifying
    @Query(
        """
        update wallet_item
        set 
            balance = balance + :balance,
            updated_at = CURRENT_TIMESTAMP
        where id = :id
    """,
    )
    override fun addBalanceById(
        id: UUID,
        balance: BigDecimal,
    ): Mono<Long>
}
