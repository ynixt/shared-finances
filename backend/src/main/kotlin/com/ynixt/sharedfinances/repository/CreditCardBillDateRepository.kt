package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.CreditCardBillDate
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate

interface CreditCardBillDateRepository : CrudRepository<CreditCardBillDate, Long> {
    fun getOneByBillDateAndCreditCardId(billDate: LocalDate, creditCardId: Long): CreditCardBillDate?
}
