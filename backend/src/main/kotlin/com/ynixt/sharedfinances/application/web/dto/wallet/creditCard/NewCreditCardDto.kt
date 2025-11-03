package com.ynixt.sharedfinances.application.web.dto.wallet.creditCard

import java.math.BigDecimal

data class NewCreditCardDto(
    val name: String,
    val currency: String,
    val totalLimit: BigDecimal,
    val dueDay: Int,
    val daysBetweenDueAndClosing: Int,
    val dueOnNextBusinessDay: Boolean = true,
)
