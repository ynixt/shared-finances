package com.ynixt.sharedfinances.application.web.dto.wallet.creditCard

data class EditCreditCardDto(
    val newName: String,
    val newEnabled: Boolean,
    val newCurrency: String,
    val newTotalLimit: java.math.BigDecimal,
    val newDueDay: Int,
    val newDaysBetweenDueAndClosing: Int,
    val newDueOnNextBusinessDay: Boolean,
)
