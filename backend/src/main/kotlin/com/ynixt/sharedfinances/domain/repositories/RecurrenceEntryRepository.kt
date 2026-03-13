package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface RecurrenceEntryRepository : EntityRepository<RecurrenceEntryEntity> {
    fun updateNextBillDate(
        id: UUID,
        nextBillDate: LocalDate?,
    ): Mono<Int>

    fun findAllByWalletEventId(walletEventId: UUID): Flux<RecurrenceEntryEntity>
}
