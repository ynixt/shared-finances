package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.queue.producer.GenerateEntryRecurrenceQueueProducer
import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import com.ynixt.sharedfinances.domain.services.EntryRecurrenceService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EntryRecurrenceServiceImpl(
    private val repository: EntryRecurrenceConfigRepository,
    private val queueProducer: GenerateEntryRecurrenceQueueProducer,
) : EntryRecurrenceService {
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
            RecurrenceType.SINGLE -> null
            RecurrenceType.DAILY -> lastExecution.plusDays(1)
            RecurrenceType.WEEKLY -> lastExecution.plusWeeks(1)
            RecurrenceType.MONTHLY -> lastExecution.plusMonths(1)
            RecurrenceType.YEARLY -> lastExecution.plusYears(1)
        }
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
}
