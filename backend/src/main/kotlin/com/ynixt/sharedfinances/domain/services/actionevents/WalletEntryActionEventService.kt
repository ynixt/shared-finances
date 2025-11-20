package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntryActionEventService {
    fun sendInsertedWalletEntry(
        userId: UUID,
        walletEntry: WalletEntryEntity,
    ): Mono<Long>
}
