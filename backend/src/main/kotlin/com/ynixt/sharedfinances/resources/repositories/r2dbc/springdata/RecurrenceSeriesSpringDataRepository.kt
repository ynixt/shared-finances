package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceSeriesEntity
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface RecurrenceSeriesSpringDataRepository :
    RecurrenceSeriesRepository,
    R2dbcRepository<RecurrenceSeriesEntity, String> {
    @Modifying
    @Query(
        """
        update recurrence_series
        set
            qty_total = :qtyTotal,
            updated_at = CURRENT_TIMESTAMP
        where id = :id
    """,
    )
    override fun updateQtyTotal(
        id: UUID,
        qtyTotal: Int?,
    ): Mono<Long>

    @Modifying
    @Query(
        """
        update recurrence_series
        set
            qty_total = coalesce(qty_total, 0) + :amount,
            updated_at = CURRENT_TIMESTAMP
        where id = :id
    """,
    )
    override fun incrementQtyTotal(
        id: UUID,
        amount: Int,
    ): Mono<Long>
}
