package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntryActionEventService {
    fun sendInsertedWalletEntry(
        userId: UUID,
        walletEntry: MinimumWalletEntry,
    ): Mono<Long>
}
