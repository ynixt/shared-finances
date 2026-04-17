package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import java.util.UUID

interface WalletEventListService {
    suspend fun list(
        userId: UUID,
        request: ListEntryRequest,
    ): CursorPage<EventListResponse>

    suspend fun findById(
        userId: UUID,
        walletEventId: UUID,
    ): EventListResponse?

    suspend fun findScheduledByRecurrenceConfigId(
        userId: UUID,
        recurrenceConfigId: UUID,
    ): EventListResponse?

    suspend fun convertEntityToEntryListResponse(
        events: List<MinimumWalletEventEntity>,
        simulateBillForRecurrence: Boolean = false,
    ): List<EventListResponse>

    suspend fun convertEntityToEntryListResponse(
        event: MinimumWalletEventEntity,
        simulateBillForRecurrence: Boolean = false,
    ): EventListResponse
}
