package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import java.time.LocalDate

interface EntryRecurrenceService {
    fun calculateNextExecution(
        lastExecution: LocalDate,
        periodicity: RecurrenceType,
        qtyExecuted: Int,
        qtyLimit: Int?,
    ): LocalDate?

    fun calculateEndDate(
        lastExecution: LocalDate,
        periodicity: RecurrenceType,
        qtyExecuted: Int,
        qtyLimit: Int?,
    ): LocalDate?

    suspend fun queueAllPendingOfExecution(): Int
}
