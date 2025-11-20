package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest

interface WalletEntryDtoMapper {
    fun fromNewDtoToNewRequest(from: NewEntryDto): NewEntryRequest
}
