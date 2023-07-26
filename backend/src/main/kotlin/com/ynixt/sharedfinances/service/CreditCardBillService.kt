package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.CreditCard
import com.ynixt.sharedfinances.entity.CreditCardBillDate
import java.time.LocalDate

interface CreditCardBillService {
    fun getOrCreate(creditCardBillDateValue: LocalDate, creditCard: CreditCard): CreditCardBillDate
}
