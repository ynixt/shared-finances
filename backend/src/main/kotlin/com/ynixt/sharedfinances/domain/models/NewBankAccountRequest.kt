package com.ynixt.sharedfinances.domain.models

import java.math.BigDecimal

data class NewBankAccountRequest(
    val name: String,
    val balance: BigDecimal,
    val currency: String,
)
