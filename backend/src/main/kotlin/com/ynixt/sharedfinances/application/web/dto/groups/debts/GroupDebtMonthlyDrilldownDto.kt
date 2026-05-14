package com.ynixt.sharedfinances.application.web.dto.groups.debts

import java.math.BigDecimal
import java.util.UUID

data class GroupDebtMonthlyDrilldownDto(
    val payerId: UUID,
    val receiverId: UUID,
    val currency: String,
    val month: String,
    val netAmount: BigDecimal,
    val chargeDelta: BigDecimal,
    val settlementDelta: BigDecimal,
    val manualAdjustmentDelta: BigDecimal,
    val lines: List<GroupDebtMovementDto>,
)
