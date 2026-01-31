package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import java.time.LocalDate
import java.util.UUID

interface WalletEntryCreateService {
    suspend fun create(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): MinimumWalletEntry?

    suspend fun createFromRecurrenceConfig(
        recurrenceConfigId: UUID,
        date: LocalDate,
    ): MinimumWalletEntry?
}
