package com.ynixt.sharedfinances.model.dto.bankAccount

import java.math.BigDecimal

data class BankAccountSummaryDto(
    val balance: BigDecimal,
    val expenses: BigDecimal,
    val revenues: BigDecimal,
)
