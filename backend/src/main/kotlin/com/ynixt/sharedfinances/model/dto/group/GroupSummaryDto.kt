package com.ynixt.sharedfinances.model.dto.group

import java.math.BigDecimal
import java.math.MathContext

data class GroupSummaryDto(val expenses: List<GroupSummaryByUserDto>) {
    val totalExpenses = expenses.sumOf { it.expense }

    init {
        expenses.forEach {
            it.percentageOfExpenses =
                (((it.expense * BigDecimal(100) / totalExpenses)).round(MathContext(2)))
        }
    }
}

data class GroupSummaryByUserDto(
    val expense: BigDecimal, val userId: Long
) {
    var percentageOfExpenses: BigDecimal = BigDecimal.ZERO
}
