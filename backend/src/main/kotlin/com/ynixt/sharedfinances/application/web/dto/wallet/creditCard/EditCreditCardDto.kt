package com.ynixt.sharedfinances.application.web.dto.wallet.creditCard

import java.math.BigDecimal

data class EditCreditCardDto(
    val newName: String,
    val newEnabled: Boolean,
    val newCurrency: String,
    val newTotalLimit: BigDecimal,
    val newDueDay: Int,
    val newDaysBetweenDueAndClosing: Int,
    val newDueOnNextBusinessDay: Boolean,
)
