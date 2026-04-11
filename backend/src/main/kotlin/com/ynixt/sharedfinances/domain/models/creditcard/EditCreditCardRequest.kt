package com.ynixt.sharedfinances.domain.models.creditcard

import java.math.BigDecimal

data class EditCreditCardRequest(
    val newName: String,
    val newEnabled: Boolean,
    val newCurrency: String,
    val newShowOnDashboard: Boolean = true,
    val newTotalLimit: BigDecimal,
    val newDueDay: Int,
    val newDaysBetweenDueAndClosing: Int,
    val newDueOnNextBusinessDay: Boolean,
)
