package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface RecurrenceEventSpringDataRepository :
    R2dbcRepository<RecurrenceEventEntity, String>,
    EntityRepository<RecurrenceEventEntity> {
    fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<RecurrenceEventEntity>

    @Modifying
    @Query(
        """
            delete from recurrence_event re
            where re.id in (
                select distinct ren.wallet_event_id
                from recurrence_entry ren
                join wallet_item wi on wi.id = ren.wallet_item_id
                where
                    ren.wallet_item_id = :walletItemId
                    and wi.user_id = :userId
            )
        """,
    )
    fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long>

    @Modifying
    @Query(
        """
            update recurrence_event
            set
                last_execution = next_execution,
                next_execution = :nextExecution,
                qty_executed = qty_executed + 1,
                updated_at = CURRENT_TIMESTAMP
            where
                id = :id
                and next_execution = :oldNextExecution
        """,
    )
    fun updateConfigCausedByExecution(
        id: UUID,
        oldNextExecution: LocalDate,
        nextExecution: LocalDate?,
    ): Mono<Int>
}
