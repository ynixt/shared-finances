package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntryListResponse
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntryListService {
    fun list(
        userId: UUID?,
        groupId: UUID?,
        request: ListEntryRequest,
    ): Mono<CursorPage<EntryListResponse>>
}
