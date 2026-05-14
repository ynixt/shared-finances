package com.ynixt.sharedfinances.application.web.dto.groups.debts

import java.math.BigDecimal
import java.util.UUID

data class GroupDebtPairHistoryDto(
    val firstUserId: UUID,
    val secondUserId: UUID,
    val currency: String,
    val month: String,
    val netPayerId: UUID?,
    val netReceiverId: UUID?,
    val netAmount: BigDecimal,
    val chargeDelta: BigDecimal,
    val settlementDelta: BigDecimal,
    val manualAdjustmentDelta: BigDecimal,
    val lines: List<GroupDebtMovementDto>,
)
