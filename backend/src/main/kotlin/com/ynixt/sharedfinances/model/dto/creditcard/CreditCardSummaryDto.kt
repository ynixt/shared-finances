package com.ynixt.sharedfinances.model.dto.creditcard

import java.math.BigDecimal

data class CreditCardSummaryDto(
    val bill: BigDecimal,
    val expenses: BigDecimal,
    val payments: BigDecimal,
    val paymentsOfThisBill: BigDecimal,
    val expensesOfThisBill: BigDecimal,
)
