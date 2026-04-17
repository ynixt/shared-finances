package com.ynixt.sharedfinances.domain.models.dashboard

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class OverviewExecutedExpenseFactSummary(
    val month: YearMonth,
    val walletItemId: UUID,
    val walletItemName: String,
    val walletItemType: WalletItemType,
    val groupId: UUID?,
    val groupName: String?,
    val categoryId: UUID?,
    val categoryName: String?,
    val currency: String,
    val expense: BigDecimal,
)
