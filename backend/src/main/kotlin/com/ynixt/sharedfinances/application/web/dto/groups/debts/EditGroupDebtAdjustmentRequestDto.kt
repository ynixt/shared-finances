package com.ynixt.sharedfinances.application.web.dto.groups.debts

import java.math.BigDecimal

data class EditGroupDebtAdjustmentRequestDto(
    val amountDelta: BigDecimal,
    val note: String? = null,
)
