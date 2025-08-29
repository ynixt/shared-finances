package com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount

import java.math.BigDecimal

data class NewBankAccountDto(
    val name: String,
    val balance: BigDecimal?,
)
