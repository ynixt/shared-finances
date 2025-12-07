package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardBillDto
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill

interface CreditCardBillDtoMapper {
    fun toDto(creditCardBill: CreditCardBill): CreditCardBillDto
}
