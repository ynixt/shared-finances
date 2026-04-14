package com.ynixt.sharedfinances.resources.services.walletentry.recurrence

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.queue.producer.GenerateEntryRecurrenceQueueProducer
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.resources.services.EntityServiceImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class RecurrenceServiceImpl(
    override val repository: RecurrenceEventRepository,
    private val queueProducer: GenerateEntryRecurrenceQueueProducer,
    private val clock: Clock,
) : EntityServiceImpl<RecurrenceEventEntity, RecurrenceEventEntity>(),
    RecurrenceService {
    override fun findAllEntryByWalletId(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID,
        userId: UUID?,
        groupId: UUID?,
        sort: Sort,
    ): Flow<RecurrenceEventEntity> {
        require((userId != null) xor (groupId != null))

        return repository
            .findAll(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                billDate = billDate,
                walletItemId = walletItemId,
                userId = userId,
                groupId = groupId,
                sort = sort,
            ).asFlow()
    }

    override fun findAllEntryByUserId(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID,
        sort: Sort,
    ): Flow<RecurrenceEventEntity> =
        repository
            .findAll(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                billDate = null,
                walletItemId = null,
                userId = userId,
                groupId = null,
                sort = sort,
            ).asFlow()

    override fun findAllEntryByUserIds(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userIds: Set<UUID>,
        sort: Sort,
    ): Flow<RecurrenceEventEntity> =
        repository
            .findAllByUserIds(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                userIds = userIds,
                sort = sort,
            ).asFlow()

    override fun findAllEntryByGroupId(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        groupId: UUID,
        sort: Sort,
    ): Flow<RecurrenceEventEntity> =
        repository
            .findAll(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                billDate = null,
                walletItemId = null,
                userId = null,
                groupId = groupId,
                sort = sort,
            ).asFlow()

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
        val itemsFlow = repository.findAllByNextExecutionLessThanEqual(LocalDate.now(clock)).asFlow()

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

    override fun calculateNextDate(
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
