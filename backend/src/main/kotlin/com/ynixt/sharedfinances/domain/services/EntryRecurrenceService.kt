package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import reactor.core.publisher.Mono
import java.time.LocalDate

interface EntryRecurrenceService {
    fun calculateNextDate(
        lastExecution: LocalDate,
        periodicity: RecurrenceType,
        qtyExecuted: Int,
        qtyLimit: Int?,
    ): LocalDate?

    fun queueAllPendingOfExecution(): Mono<Int>
}
