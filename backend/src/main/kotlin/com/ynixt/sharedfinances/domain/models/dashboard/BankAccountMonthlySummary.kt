package com.ynixt.sharedfinances.domain.models.dashboard

import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class BankAccountMonthlySummary(
    val walletItemId: UUID,
    val month: YearMonth,
    val net: BigDecimal,
    val cashIn: BigDecimal,
    val cashOut: BigDecimal,
)
