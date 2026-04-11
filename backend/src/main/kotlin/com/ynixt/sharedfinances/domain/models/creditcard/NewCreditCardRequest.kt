package com.ynixt.sharedfinances.domain.models.creditcard

import java.math.BigDecimal

data class NewCreditCardRequest(
    val name: String,
    val currency: String,
    val totalLimit: BigDecimal,
    val dueDay: Int,
    val daysBetweenDueAndClosing: Int,
    val dueOnNextBusinessDay: Boolean = true,
    val showOnDashboard: Boolean = true,
)
