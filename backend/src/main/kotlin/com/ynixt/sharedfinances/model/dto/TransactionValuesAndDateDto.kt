package com.ynixt.sharedfinances.model.dto

import java.math.BigDecimal

data class TransactionValuesAndDateDto(
    val date: String,
    val balance: BigDecimal,
    val expenses: BigDecimal,
    val revenues: BigDecimal,
)

data class TransactionValuesGroupChartDto(
    private val allValuesByUser: List<TransactionValuesAndDateAndUserNameDto>,
    val values: List<TransactionValuesAndDateDto>
) {
    val valuesByUser = allValuesByUser.groupBy { it.userId }
}

data class TransactionValuesAndDateAndUserNameDto(
    val date: String,
    val balance: BigDecimal,
    val expenses: BigDecimal,
    val revenues: BigDecimal,
    val userId: Long,
)
