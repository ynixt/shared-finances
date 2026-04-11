package com.ynixt.sharedfinances.domain.models.bankaccount

import java.math.BigDecimal

data class NewBankAccountRequest(
    val name: String,
    val balance: BigDecimal,
    val currency: String,
    val showOnDashboard: Boolean = true,
)
