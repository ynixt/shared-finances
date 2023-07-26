package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.CreditCard
import com.ynixt.sharedfinances.entity.CreditCardBillDate
import com.ynixt.sharedfinances.repository.CreditCardBillDateRepository
import com.ynixt.sharedfinances.service.CreditCardBillService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CreditCardBillServiceImpl(private val creditCardBillDateRepository: CreditCardBillDateRepository) :
    CreditCardBillService {
    override fun getOrCreate(creditCardBillDateValue: LocalDate, creditCard: CreditCard): CreditCardBillDate {
        val creditCardBillDate =
            creditCardBillDateRepository.getOneByBillDateAndCreditCardId(creditCardBillDateValue, creditCard.id!!)

        return creditCardBillDate ?: creditCardBillDateRepository.save(
            CreditCardBillDate(
                billDate = creditCardBillDateValue,
                creditCard = creditCard
            )
        )
    }
}
