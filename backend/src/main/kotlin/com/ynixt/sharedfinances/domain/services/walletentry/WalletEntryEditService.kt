package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.models.walletentry.EditScheduledEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import java.util.UUID

interface WalletEntryEditService {
    suspend fun editOneOff(
        userId: UUID,
        walletEventId: UUID,
        request: NewEntryRequest,
    ): MinimumWalletEventEntity?

    suspend fun editScheduled(
        userId: UUID,
        recurrenceConfigId: UUID,
        request: EditScheduledEntryRequest,
    ): MinimumWalletEventEntity?
}
