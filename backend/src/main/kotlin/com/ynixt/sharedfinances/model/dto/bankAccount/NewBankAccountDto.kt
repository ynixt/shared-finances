package com.ynixt.sharedfinances.model.dto.bankAccount

data class NewBankAccountDto(
    val name: String,
    val enabled: Boolean,
    val displayOnGroup: Boolean
)
