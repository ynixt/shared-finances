package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.CreditCard
import com.ynixt.sharedfinances.entity.CreditCardBillDate
import java.time.LocalDate

interface CreditCardBillService {
    fun getNextBillDateValue(currentBillDateValue: LocalDate, creditCardClosingDay: Int, next: Int = 1): LocalDate
    fun getOrCreate(creditCardBillDateValue: LocalDate, creditCard: CreditCard): CreditCardBillDate
}
