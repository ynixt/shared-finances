package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.queue.producer.GenerateEntryRecurrenceQueueProducer
import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EntryRecurrenceServiceImpl(
    private val repository: EntryRecurrenceConfigRepository,
    private val queueProducer: GenerateEntryRecurrenceQueueProducer,
) : EntryRecurrenceService {
    override fun calculateNextExecution(
        lastExecution: LocalDate,
        periodicity: RecurrenceType,
        qtyExecuted: Int,
        qtyLimit: Int?,
    ): LocalDate? {
        if (qtyExecuted == qtyLimit) {
            return null
        }

        return calculateNextDate(
            lastExecution = lastExecution,
            periodicity = periodicity,
        )
    }

    override fun calculateEndDate(
        lastExecution: LocalDate,
        periodicity: RecurrenceType,
        qtyExecuted: Int,
        qtyLimit: Int?,
    ): LocalDate? {
        if (qtyLimit == null) return null

        var endDate = lastExecution
        val remainingQty = qtyLimit - qtyExecuted

        repeat(remainingQty) {
            endDate = calculateNextDate(endDate, periodicity)
        }

        return endDate
    }

    override suspend fun queueAllPendingOfExecution(): Int {
        val itemsFlow = repository.findAllByNextExecutionLessThanEqual(LocalDate.now()).asFlow()

        itemsFlow.collect {
            queueProducer.send(
                GenerateEntryRecurrenceRequestDto(
                    entryRecurrenceConfigId = it.id!!,
                    date = it.nextExecution!!,
                ),
            )
        }

        return itemsFlow.toList().size
    }

    private fun calculateNextDate(
        lastExecution: LocalDate,
        periodicity: RecurrenceType,
    ): LocalDate =
        when (periodicity) {
            RecurrenceType.SINGLE -> lastExecution
            RecurrenceType.DAILY -> lastExecution.plusDays(1)
            RecurrenceType.WEEKLY -> lastExecution.plusWeeks(1)
            RecurrenceType.MONTHLY -> lastExecution.plusMonths(1)
            RecurrenceType.YEARLY -> lastExecution.plusYears(1)
        }
}
