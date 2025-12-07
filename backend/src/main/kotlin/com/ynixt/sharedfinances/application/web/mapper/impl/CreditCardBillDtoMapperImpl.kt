package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardBillDto
import com.ynixt.sharedfinances.application.web.mapper.CreditCardBillDtoMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class CreditCardBillDtoMapperImpl : CreditCardBillDtoMapper {
    private object ToDtoMapper : ObjectMappie<CreditCardBill, CreditCardBillDto>() {
        override fun map(from: CreditCardBill) =
            mapping {
            }
    }

    override fun toDto(creditCardBill: CreditCardBill): CreditCardBillDto = ToDtoMapper.map(creditCardBill)
}
