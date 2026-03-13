package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import java.util.UUID

interface WalletEventActionEventService {
    suspend fun sendInsertedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    )
}
