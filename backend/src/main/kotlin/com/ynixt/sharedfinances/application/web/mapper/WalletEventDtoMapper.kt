package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse

interface WalletEventDtoMapper {
    fun fromListResponseToListDto(from: EventListResponse): EventForListDto
}
