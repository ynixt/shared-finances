package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceSeriesEntity
import reactor.core.publisher.Mono
import java.util.UUID

interface RecurrenceSeriesRepository : EntityRepository<RecurrenceSeriesEntity> {
    fun updateQtyTotal(
        id: UUID,
        qtyTotal: Int?,
    ): Mono<Long>

    fun incrementQtyTotal(
        id: UUID,
        amount: Int = 1,
    ): Mono<Long>
}
