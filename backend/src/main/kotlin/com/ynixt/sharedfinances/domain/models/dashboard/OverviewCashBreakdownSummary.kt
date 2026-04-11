package com.ynixt.sharedfinances.domain.models.dashboard

import java.math.BigDecimal
import java.util.UUID

data class OverviewCashBreakdownSummary(
    val direction: OverviewCashDirection,
    val categoryId: UUID?,
    val categoryName: String?,
    val currency: String,
    val amount: BigDecimal,
)

enum class OverviewCashDirection {
    IN,
    OUT,
}
