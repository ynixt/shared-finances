package com.ynixt.sharedfinances.domain.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.walletentry.SimulatedOccurrence
import java.time.LocalDate
import java.util.UUID

interface RecurrenceOccurrenceSimulationService {
    suspend fun buildOccurrences(
        config: RecurrenceEventEntity,
        walletItems: List<WalletItem>,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        askedBillDate: LocalDate?,
        askedWalletItemId: UUID?,
        requestUserId: UUID?,
    ): List<SimulatedOccurrence>
}
