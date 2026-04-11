package com.ynixt.sharedfinances.domain.models.bankaccount

data class EditBankAccountRequest(
    val newName: String,
    val newEnabled: Boolean,
    val newCurrency: String,
    val newShowOnDashboard: Boolean = true,
)
