package com.ynixt.sharedfinances.model.dto.creditcard

import java.math.BigDecimal

data class CreditCardDto(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val closingDay: Int,
    val paymentDay: Int,
    val limit: BigDecimal,
    val availableLimit: BigDecimal?,
    val enabled: Boolean,
    val displayOnGroup: Boolean,
)
