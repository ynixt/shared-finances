package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntryCreateService {
    fun create(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Mono<MinimumWalletEntry>
}
