package com.ynixt.sharedfinances.domain.models.dashboard

import java.math.BigDecimal
import java.util.UUID

data class OverviewExpenseBreakdownSummary(
    val groupId: UUID?,
    val groupName: String?,
    val categoryId: UUID?,
    val categoryName: String?,
    val currency: String,
    val expense: BigDecimal,
)
