package com.ynixt.sharedfinances.domain.models.dashboard

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import java.math.BigDecimal
import java.util.UUID

data class OverviewExpenseSourceSummary(
    val walletItemId: UUID,
    val walletItemName: String,
    val walletItemType: WalletItemType,
    val currency: String,
    val expense: BigDecimal,
)
