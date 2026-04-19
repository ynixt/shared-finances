package com.ynixt.sharedfinances.application.web.dto.groups.debts

import java.math.BigDecimal

data class GroupDebtMonthlyCompositionDto(
    val month: String,
    val netAmount: BigDecimal,
    val chargeDelta: BigDecimal,
    val settlementDelta: BigDecimal,
    val manualAdjustmentDelta: BigDecimal,
)
