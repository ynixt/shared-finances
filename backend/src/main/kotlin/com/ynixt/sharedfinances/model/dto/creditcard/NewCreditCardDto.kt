package com.ynixt.sharedfinances.model.dto.creditcard

import java.math.BigDecimal

data class NewCreditCardDto(
    val name: String,
    val limit: BigDecimal,
    val closingDay: Int,
    val paymentDay: Int,
    val enabled: Boolean,
    val displayOnGroup: Boolean,
)
