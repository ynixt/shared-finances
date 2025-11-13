package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemSearchResponseDto
import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse

interface WalletItemDtoMapper {
    fun searchResponseToDto(from: WalletItemSearchResponse): WalletItemSearchResponseDto
}
