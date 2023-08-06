package com.ynixt.sharedfinances.model.dto.user

import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountDto

data class CurrentUserDto(
    val id: Long? = null,
    val email: String,
    val name: String,
    val photoUrl: String?,
    val bankAccounts: List<BankAccountDto>,
    val lang: String
)
