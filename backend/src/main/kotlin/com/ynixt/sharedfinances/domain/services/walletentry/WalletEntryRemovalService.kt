package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.models.walletentry.DeleteScheduledEntryRequest
import java.util.UUID

interface WalletEntryRemovalService {
    suspend fun deleteOneOff(
        userId: UUID,
        walletEventId: UUID,
    ): MinimumWalletEventEntity?

    suspend fun deleteScheduled(
        userId: UUID,
        recurrenceConfigId: UUID,
        request: DeleteScheduledEntryRequest,
    ): MinimumWalletEventEntity?
}
