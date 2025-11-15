package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemSearchResponseDto
import com.ynixt.sharedfinances.domain.models.WalletItem

interface WalletItemDtoMapper {
    fun searchResponseToDto(from: WalletItem): WalletItemSearchResponseDto
}
