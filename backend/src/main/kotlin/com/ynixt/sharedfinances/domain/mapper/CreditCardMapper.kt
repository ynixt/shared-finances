package com.ynixt.sharedfinances.domain.mapper

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard

interface CreditCardMapper {
    fun toEntity(from: CreditCard): WalletItemEntity

    fun toModel(from: WalletItemEntity): CreditCard
}
