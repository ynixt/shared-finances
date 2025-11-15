package com.ynixt.sharedfinances.domain.mapper.impl

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.mapper.BankAccountMapper
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class BankAccountMapperImpl : BankAccountMapper {
    override fun toEntity(from: BankAccount): WalletItemEntity = BankAccountToEntityMapper.map(from)

    override fun toModel(from: WalletItemEntity): BankAccount = BankAccountToModelMapper.map(from)

    private object BankAccountToEntityMapper : ObjectMappie<BankAccount, WalletItemEntity>() {
        override fun map(from: BankAccount) =
            mapping {
                to::totalLimit fromValue null
                to::dueDay fromValue null
                to::daysBetweenDueAndClosing fromValue null
                to::dueOnNextBusinessDay fromValue null
            }
    }

    private object BankAccountToModelMapper : ObjectMappie<WalletItemEntity, BankAccount>() {
        override fun map(from: WalletItemEntity) =
            mapping {
            }
    }
}
