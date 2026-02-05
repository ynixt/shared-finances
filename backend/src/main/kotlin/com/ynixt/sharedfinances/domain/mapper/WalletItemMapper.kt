package com.ynixt.sharedfinances.domain.mapper

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.models.WalletItem

interface WalletItemMapper {
    fun toModel(from: WalletItemEntity): WalletItem

    fun fromModel(from: WalletItem): WalletItemEntity
}
