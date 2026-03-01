package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface EntryRecurrenceConfigRepository : EntityRepository<EntryRecurrenceConfigEntity> {
    fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<EntryRecurrenceConfigEntity>

    fun updateConfigCausedByExecution(
        id: UUID,
        oldNextExecution: LocalDate,
        nextExecution: LocalDate?,
        nextOriginBillDate: LocalDate?,
        nextTargetBillDate: LocalDate?,
    ): Mono<Int>

    fun findAll(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        originId: UUID?,
        targetId: UUID?,
        userId: UUID?,
        groupId: UUID?,
        sort: Sort = Sort.unsorted(),
    ): Flux<EntryRecurrenceConfigEntity>
}
