package com.ynixt.sharedfinances.domain.mapper

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill

interface CreditCardBillMapper {
    fun toModel(entity: CreditCardBillEntity): CreditCardBill
}
