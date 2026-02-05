package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntryListResponse
import java.util.UUID

interface WalletEntryListService {
    suspend fun list(
        userId: UUID,
        groupId: UUID?,
        request: ListEntryRequest,
    ): CursorPage<EntryListResponse>

    suspend fun convertEntityToEntryListResponse(
        items: List<MinimumWalletEntry>,
        simulateBillForRecurrence: Boolean = false,
    ): List<EntryListResponse>

    suspend fun convertEntityToEntryListResponse(
        item: MinimumWalletEntry,
        simulateBillForRecurrence: Boolean = false,
    ): EntryListResponse
}
