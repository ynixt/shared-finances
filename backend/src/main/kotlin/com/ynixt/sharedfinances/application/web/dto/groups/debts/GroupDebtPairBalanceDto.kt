package com.ynixt.sharedfinances.application.web.dto.groups.debts

import java.math.BigDecimal
import java.util.UUID

data class GroupDebtPairBalanceDto(
    val payerId: UUID,
    val receiverId: UUID,
    val currency: String,
    val outstandingAmount: BigDecimal,
    val monthlyComposition: List<GroupDebtMonthlyCompositionDto>,
)
