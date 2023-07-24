package com.ynixt.sharedfinances.model.dto.bankAccount

import java.math.BigDecimal

data class BankAccountDto(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val balance: BigDecimal,
    val enabled: Boolean,
    val displayOnGroup: Boolean
)
