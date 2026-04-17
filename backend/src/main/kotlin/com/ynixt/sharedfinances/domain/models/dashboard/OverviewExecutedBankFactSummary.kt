package com.ynixt.sharedfinances.domain.models.dashboard

import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class OverviewExecutedBankFactSummary(
    val walletItemId: UUID,
    val month: YearMonth,
    val categoryId: UUID?,
    val categoryName: String?,
    val currency: String,
    val net: BigDecimal,
    val cashIn: BigDecimal,
    val cashOut: BigDecimal,
)
