package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import java.util.UUID

interface WalletEntryActionEventService {
    suspend fun sendInsertedWalletEntry(
        userId: UUID,
        walletEntry: MinimumWalletEntry,
    )
}
