package com.ynixt.sharedfinances.domain.mapper.impl

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.mapper.CreditCardMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class CreditCardMapperImpl : CreditCardMapper {
    override fun toEntity(from: CreditCard): WalletItemEntity = CreditCardToEntityMapper.map(from)

    override fun toModel(from: WalletItemEntity): CreditCard = CreditCardToModelMapper.map(from)

    private object CreditCardToEntityMapper : ObjectMappie<CreditCard, WalletItemEntity>() {
        override fun map(from: CreditCard) =
            mapping {
            }
    }

    private object CreditCardToModelMapper : ObjectMappie<WalletItemEntity, CreditCard>() {
        override fun map(from: WalletItemEntity) =
            mapping {
                to::totalLimit fromPropertyNotNull from::totalLimit
                to::dueDay fromPropertyNotNull from::dueDay
                to::daysBetweenDueAndClosing fromPropertyNotNull from::daysBetweenDueAndClosing
                to::dueOnNextBusinessDay fromPropertyNotNull from::dueOnNextBusinessDay
            }
    }
}
