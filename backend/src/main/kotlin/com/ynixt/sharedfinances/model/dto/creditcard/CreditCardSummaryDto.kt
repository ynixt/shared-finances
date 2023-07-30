package com.ynixt.sharedfinances.model.dto.creditcard

import java.math.BigDecimal

data class CreditCardSummaryDto(
    val bill: BigDecimal = BigDecimal.ZERO,
    val expenses: BigDecimal = BigDecimal.ZERO,
    val payments: BigDecimal = BigDecimal.ZERO,
    val paymentsOfThisBill: BigDecimal = BigDecimal.ZERO,
    val expensesOfThisBill: BigDecimal = BigDecimal.ZERO,
)
