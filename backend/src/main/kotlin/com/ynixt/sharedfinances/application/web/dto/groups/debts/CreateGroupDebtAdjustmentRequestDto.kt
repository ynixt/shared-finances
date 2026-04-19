package com.ynixt.sharedfinances.application.web.dto.groups.debts

import java.math.BigDecimal
import java.util.UUID

data class CreateGroupDebtAdjustmentRequestDto(
    val payerId: UUID,
    val receiverId: UUID,
    val month: String,
    val currency: String,
    val amountDelta: BigDecimal,
    val note: String? = null,
)
