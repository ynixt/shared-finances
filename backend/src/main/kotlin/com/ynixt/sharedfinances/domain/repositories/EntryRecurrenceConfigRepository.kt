package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
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
    ): Mono<Int>
}
