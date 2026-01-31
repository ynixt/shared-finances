package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.models.SummaryEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummary
import java.util.UUID

interface WalletEntrySummaryService {
    suspend fun summary(
        userId: UUID,
        groupId: UUID?,
        request: SummaryEntryRequest,
    ): EntrySummary?
}
