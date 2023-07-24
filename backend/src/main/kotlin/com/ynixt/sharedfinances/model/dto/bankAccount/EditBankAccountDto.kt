package com.ynixt.sharedfinances.model.dto.bankAccount

data class EditBankAccountDto(
    val name: String,
    val enabled: Boolean,
    val displayOnGroup: Boolean
)
