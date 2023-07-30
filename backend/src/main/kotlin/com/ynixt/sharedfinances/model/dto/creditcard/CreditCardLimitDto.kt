package com.ynixt.sharedfinances.model.dto.creditcard

import java.math.BigDecimal

data class CreditCardLimitDto(
    val id: Long, val limit: BigDecimal = BigDecimal.ZERO, val availableLimit: BigDecimal = BigDecimal.ZERO,
)
