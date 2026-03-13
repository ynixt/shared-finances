package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import java.time.LocalDate
import java.util.UUID

interface WalletEntryCreateService {
    suspend fun create(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): MinimumWalletEventEntity?

    suspend fun createFromRecurrenceConfig(
        recurrenceConfigId: UUID,
        date: LocalDate,
    ): MinimumWalletEventEntity?
}
