package com.ynixt.sharedfinances.domain.mapper

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount

interface BankAccountMapper {
    fun toEntity(from: BankAccount): WalletItemEntity

    fun toModel(from: WalletItemEntity): BankAccount
}
