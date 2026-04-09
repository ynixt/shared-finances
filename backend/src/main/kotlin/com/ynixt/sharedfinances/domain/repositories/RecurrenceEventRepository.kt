package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface RecurrenceEventRepository : EntityRepository<RecurrenceEventEntity> {
    fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<RecurrenceEventEntity>

    fun findAllBySeriesId(seriesId: UUID): Flux<RecurrenceEventEntity>

    fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long>

    fun updateConfigCausedByExecution(
        id: UUID,
        oldNextExecution: LocalDate,
        nextExecution: LocalDate?,
    ): Mono<Int>

    fun findAll(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        userId: UUID?,
        groupId: UUID?,
        sort: Sort = Sort.unsorted(),
    ): Flux<RecurrenceEventEntity>
}
