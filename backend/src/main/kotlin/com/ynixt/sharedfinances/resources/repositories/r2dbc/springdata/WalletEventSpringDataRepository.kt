package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEventSpringDataRepository : R2dbcRepository<WalletEventEntity, String> {
    fun findOneByRecurrenceEventIdAndDate(
        recurrenceEventId: UUID,
        date: java.time.LocalDate,
    ): Mono<WalletEventEntity>

    fun findAllByRecurrenceEventId(recurrenceEventId: UUID): Flux<WalletEventEntity>

    @Modifying
    @Query(
        """
            delete from wallet_event we
            where we.id in (
                select distinct wen.wallet_event_id
                from wallet_entry wen
                join wallet_item wi on wi.id = wen.wallet_item_id
                where
                    wen.wallet_item_id = :walletItemId
                    and wi.user_id = :userId
            )
        """,
    )
    fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long>
}
