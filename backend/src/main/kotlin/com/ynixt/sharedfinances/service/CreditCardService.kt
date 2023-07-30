package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.CreditCard
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto
import com.ynixt.sharedfinances.model.dto.creditcard.*
import java.math.BigDecimal
import java.time.LocalDate

interface CreditCardService {
    fun getSummary(user: User, creditCardId: Long, maxCreditCardBillDate: LocalDate): CreditCardSummaryDto
    fun getOne(user: User, id: Long): CreditCard?
    fun listCreditCard(user: User): List<CreditCard>
    fun listCreditCardAsCreditCardDto(user: User): List<CreditCardDto>
    fun newCreditCard(user: User, newDto: NewCreditCardDto): CreditCard
    fun updateCreditCard(user: User, id: Long, updateDto: UpdateCreditCardDto): CreditCard
    fun delete(user: User, id: Long)
    fun getChartByCreditCardId(
        user: User,
        creditCardId: Long,
        minCreditCardBillDate: LocalDate?,
        maxCreditCardBillDate: LocalDate?
    ): List<TransactionValuesAndDateDto>

    fun addToAvailableLimit(targetUser: User, creditCardId: Long, delta: BigDecimal)
    fun getCurrentLimit(user: User, creditCardId: Long): CreditCardLimitDto?
}
