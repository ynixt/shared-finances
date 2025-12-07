package com.ynixt.sharedfinances.domain.mapper.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.mapper.CreditCardBillMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class CreditCardBillMapperImpl : CreditCardBillMapper {
    private object ToDtoMapper : ObjectMappie<CreditCardBillEntity, CreditCardBill>() {
        override fun map(from: CreditCardBillEntity) =
            mapping {
            }
    }

    override fun toModel(entity: CreditCardBillEntity): CreditCardBill = ToDtoMapper.map(entity)
}
