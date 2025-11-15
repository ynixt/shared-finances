package com.ynixt.sharedfinances.domain.mapper.impl

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.BankAccountMapper
import com.ynixt.sharedfinances.domain.mapper.CreditCardMapper
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import org.springframework.stereotype.Component

@Component
class WalletItemMapperImpl(
    private val bankAccountMapper: BankAccountMapper,
    private val creditCardMapper: CreditCardMapper,
) : WalletItemMapper {
    override fun toModel(from: WalletItemEntity): WalletItem =
        when (from.type) {
            WalletItemType.BANK_ACCOUNT -> bankAccountMapper.toModel(from)
            WalletItemType.CREDIT_CARD -> creditCardMapper.toModel(from)
        }
}
