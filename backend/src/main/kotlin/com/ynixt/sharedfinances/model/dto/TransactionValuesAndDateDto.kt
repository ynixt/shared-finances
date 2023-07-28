package com.ynixt.sharedfinances.model.dto

import java.math.BigDecimal

data class TransactionValuesAndDateDto(
    val date: String,
    val balance: BigDecimal,
    val expenses: BigDecimal,
    val revenues: BigDecimal,
)
