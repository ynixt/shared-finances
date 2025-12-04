package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.walletentry.EntryForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntryListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest

interface WalletEntryDtoMapper {
    fun fromNewDtoToNewRequest(from: NewEntryDto): NewEntryRequest

    fun fromListResponseToListDto(from: EntryListResponse): EntryForListDto

    fun fromEntryListRequestDtoToModel(from: ListEntryRequestDto): ListEntryRequest
}
