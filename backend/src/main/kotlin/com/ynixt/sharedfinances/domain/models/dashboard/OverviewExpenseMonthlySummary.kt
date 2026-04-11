package com.ynixt.sharedfinances.domain.models.dashboard

import java.math.BigDecimal
import java.time.YearMonth

data class OverviewExpenseMonthlySummary(
    val month: YearMonth,
    val currency: String,
    val expense: BigDecimal,
)
