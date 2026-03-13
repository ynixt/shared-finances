package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface EntryRecurrenceConfigService {
    suspend fun getFutureValuesOfWalletItem(
        walletItemId: UUID,
        minimumEndExecution: LocalDate,
        maximumNextExecution: LocalDate,
        userId: UUID,
        groupId: UUID?,
    ): BigDecimal

    suspend fun getFutureValuesOCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupId: UUID?,
        walletItemId: UUID,
    ): BigDecimal

    suspend fun simulateGeneration(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        billDate: LocalDate?,
    ): List<EventListResponse>

    suspend fun simulateGenerationAsEntrySumResult(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
    ): List<EntrySumResult>

    suspend fun simulateGenerationForCreditCard(
        billDate: LocalDate,
        userId: UUID,
        groupId: UUID?,
        walletItemId: UUID,
    ): List<EventListResponse>

    suspend fun simulateGenerationForCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupId: UUID?,
        walletItemId: UUID?,
    ): List<EventListResponse>

    suspend fun simulateGeneration(
        config: RecurrenceEventEntity,
        walletItems: List<WalletItem>,
        user: UserEntity?,
        group: GroupEntity?,
        category: WalletEntryCategoryEntity?,
        simulateBillForRecurrence: Boolean,
    ): EventListResponse

    fun findAllByIdIn(ids: Collection<UUID>): Flow<RecurrenceEventEntity>
}
