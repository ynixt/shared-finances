package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.SummaryEntryRequestDto
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.SummaryEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummary
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest

interface WalletEntryDtoMapper {
    fun fromNewDtoToNewRequest(from: NewEntryDto): NewEntryRequest

    fun fromEntryListRequestDtoToModel(from: ListEntryRequestDto): ListEntryRequest

    fun fromEntrySummaryRequestDtoToModel(from: SummaryEntryRequestDto): SummaryEntryRequest

    fun fromSummaryModelToDto(from: EntrySummary): EntrySummaryDto

    fun fromEntryResponseToDto(from: EventListResponse.EntryResponse): EventForListDto.EntryResponseDto
}
