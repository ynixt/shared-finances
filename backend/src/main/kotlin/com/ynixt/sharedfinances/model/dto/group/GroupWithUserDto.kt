package com.ynixt.sharedfinances.model.dto.group

import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountDto
import java.math.BigDecimal

data class GroupWithUserDto(
    val id: Long? = null, val name: String, val users: List<UserForGroupWithUserDto>
)

data class UserForGroupWithUserDto(
    val id: Long? = null,
    val email: String,
    val name: String,
    val photoUrl: String?,
    val bankAccounts: List<BankAccountDto>,
    val creditCards: List<CreditCardForUserForGroupWithUserDto>
)

data class CreditCardForUserForGroupWithUserDto(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val closingDay: Int,
    val paymentDay: Int,
    val limit: BigDecimal,
    val availableLimit: BigDecimal?,
    val enabled: Boolean,
    val displayOnGroup: Boolean
)
