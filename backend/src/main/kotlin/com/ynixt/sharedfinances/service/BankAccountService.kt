package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.BankAccount
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.bankAccount.NewBankAccountDto
import java.time.ZonedDateTime

interface BankAccountService {
    fun getOne(id: Long, user: User): BankAccount?
    fun updateName(id: Long, user: User, newName: String): BankAccount
    fun deleteOne(id: Long, user: User)
    fun getSummary(user: User, bankAccountId: Long? = null, maxDate: ZonedDateTime? = null): BankAccountSummaryDto
    fun newBank(user: User, newBankAccountDto: NewBankAccountDto): BankAccount
}
