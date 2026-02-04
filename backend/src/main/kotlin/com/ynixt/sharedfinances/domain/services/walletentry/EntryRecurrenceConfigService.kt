package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.walletentry.EntryListResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface EntryRecurrenceConfigService {
    suspend fun getFutureValuesOfWalletItem(
        walletId: UUID,
        minimumEndExecution: LocalDate,
        maximumNextExecution: LocalDate,
        userId: UUID,
        groupId: UUID?,
    ): BigDecimal

    suspend fun getFutureValuesOCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupId: UUID?,
        walletId: UUID,
    ): BigDecimal

    suspend fun simulateGeneration(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID,
        groupId: UUID?,
        walletId: UUID?,
    ): List<EntryListResponse>

    suspend fun simulateGenerationForCreditCard(
        billDate: LocalDate,
        userId: UUID,
        groupId: UUID?,
        walletId: UUID,
    ): List<EntryListResponse>

    suspend fun simulateGenerationForCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupId: UUID?,
        walletId: UUID?,
    ): List<EntryListResponse>
}
