package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.models.SummaryEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummary
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntrySummaryService {
    fun summary(
        userId: UUID,
        groupId: UUID?,
        request: SummaryEntryRequest,
    ): Mono<EntrySummary>
}
