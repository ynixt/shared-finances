package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.queue.producer.GenerateEntryRecurrenceQueueProducer
import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import com.ynixt.sharedfinances.domain.services.EntryRecurrenceService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
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

    override fun queueAllPendingOfExecution(): Mono<Int> =
        repository
            .findAllByNextExecutionLessThanEqual(LocalDate.now())
            .doOnNext { config ->
                queueProducer.send(
                    GenerateEntryRecurrenceRequestDto(
                        entryRecurrenceConfigId = config.id!!,
                        date = config.nextExecution!!,
                    ),
                )
            }.collectList()
            .map { it.size }
}
