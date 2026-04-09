package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.walletentry.DeleteScheduledEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EditScheduledEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ScheduledExecutionManagerRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.SummaryEntryRequestDto
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.SummaryEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.DeleteScheduledEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EditScheduledEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummary
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.ScheduledExecutionManagerRequest

interface WalletEntryDtoMapper {
    fun fromNewDtoToNewRequest(from: NewEntryDto): NewEntryRequest

    fun fromEntryListRequestDtoToModel(from: ListEntryRequestDto): ListEntryRequest

    fun fromEntrySummaryRequestDtoToModel(from: SummaryEntryRequestDto): SummaryEntryRequest

    fun fromEditScheduledDtoToModel(from: EditScheduledEntryDto): EditScheduledEntryRequest

    fun fromDeleteScheduledDtoToModel(from: DeleteScheduledEntryDto): DeleteScheduledEntryRequest

    fun fromScheduledExecutionManagerRequestDtoToModel(from: ScheduledExecutionManagerRequestDto): ScheduledExecutionManagerRequest

    fun fromSummaryModelToDto(from: EntrySummary): EntrySummaryDto

    fun fromEntryResponseToDto(from: EventListResponse.EntryResponse): EventForListDto.EntryResponseDto
}
