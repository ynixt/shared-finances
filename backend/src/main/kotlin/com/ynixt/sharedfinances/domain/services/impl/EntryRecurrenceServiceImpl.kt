package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.services.EntryRecurrenceService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EntryRecurrenceServiceImpl : EntryRecurrenceService {
    override fun calculateNextDate(
        lastExecution: LocalDate,
        periodicity: RecurrenceType,
        qtyExecuted: Int,
        qtyLimit: Int?,
    ): LocalDate? {
        if (qtyExecuted == qtyLimit) {
            return null
        }

        return when (periodicity) {
            RecurrenceType.DAILY -> lastExecution.plusDays(1)
            RecurrenceType.WEEKLY -> lastExecution.plusWeeks(1)
            RecurrenceType.MONTHLY -> lastExecution.plusMonths(1)
            RecurrenceType.YEARLY -> lastExecution.plusYears(1)
        }
    }
}
