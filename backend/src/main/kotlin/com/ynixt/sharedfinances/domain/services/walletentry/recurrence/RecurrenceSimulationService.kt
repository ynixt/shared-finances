package com.ynixt.sharedfinances.domain.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface RecurrenceSimulationService {
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
        groupIds: Set<UUID>,
        userIds: Set<UUID> = emptySet(),
        walletItemId: UUID?,
        billDate: LocalDate?,
    ): List<EventListResponse>

    suspend fun simulateGenerationWithFilters(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        walletItemId: UUID?,
        billDate: LocalDate?,
        groupIds: Set<UUID>,
        userIds: Set<UUID> = emptySet(),
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
    ): List<EventListResponse> =
        simulateGeneration(
            minimumEndExecution = minimumEndExecution,
            maximumNextExecution = maximumNextExecution,
            userId = userId,
            groupIds = groupIds,
            userIds = userIds,
            walletItemId = walletItemId,
            billDate = billDate,
        )

    suspend fun simulateGenerationForUsers(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userIds: Set<UUID>,
        billDate: LocalDate?,
    ): List<EventListResponse>

    suspend fun simulateGenerationAsEntrySumResult(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        summaryMinimumDate: LocalDate,
    ): List<EntrySumResult>

    suspend fun simulateGenerationForCreditCard(
        billDate: LocalDate,
        userId: UUID,
        groupIds: Set<UUID>,
        userIds: Set<UUID> = emptySet(),
        walletItemId: UUID,
    ): List<EventListResponse>

    suspend fun simulateGenerationForCreditCardWithFilters(
        billDate: LocalDate,
        userId: UUID,
        walletItemId: UUID,
        groupIds: Set<UUID>,
        userIds: Set<UUID> = emptySet(),
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
    ): List<EventListResponse> =
        simulateGenerationForCreditCard(
            billDate = billDate,
            userId = userId,
            groupIds = groupIds,
            userIds = userIds,
            walletItemId = walletItemId,
        )

    suspend fun simulateGenerationForCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupIds: Set<UUID>,
        userIds: Set<UUID> = emptySet(),
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
}
