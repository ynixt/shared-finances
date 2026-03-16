package com.ynixt.sharedfinances.domain.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.util.UUID

interface RecurrenceService {
    fun findAllByIdIn(ids: Collection<UUID>): Flow<RecurrenceEventEntity>

    fun findAllEntryByWalletId(
        minimumEndExecution: LocalDate? = null,
        maximumNextExecution: LocalDate? = null,
        billDate: LocalDate? = null,
        walletItemId: UUID,
        userId: UUID? = null,
        groupId: UUID? = null,
        sort: Sort = Sort.unsorted(),
    ): Flow<RecurrenceEventEntity>

    fun findAllEntryByUserId(
        minimumEndExecution: LocalDate? = null,
        maximumNextExecution: LocalDate? = null,
        userId: UUID,
        sort: Sort = Sort.unsorted(),
    ): Flow<RecurrenceEventEntity>

    fun findAllEntryByGroupId(
        minimumEndExecution: LocalDate? = null,
        maximumNextExecution: LocalDate? = null,
        groupId: UUID,
        sort: Sort = Sort.unsorted(),
    ): Flow<RecurrenceEventEntity>

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

    fun calculateNextDate(
        lastExecution: LocalDate,
        periodicity: RecurrenceType,
    ): LocalDate

    suspend fun queueAllPendingOfExecution(): Int
}
