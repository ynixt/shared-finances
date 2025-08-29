package com.ynixt.sharedfinances.domain.models

data class EditBankAccountRequest(
    val newName: String,
    val newEnabled: Boolean,
)
