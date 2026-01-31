package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.bankaccount.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.bankaccount.NewBankAccountRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface BankAccountService {
    suspend fun findAllBanks(
        userId: UUID,
        pageable: Pageable,
    ): Page<BankAccount>

    suspend fun findBankAccount(
        userId: UUID,
        id: UUID,
    ): BankAccount?

    suspend fun newBankAccount(
        userId: UUID,
        newBankAccountRequest: NewBankAccountRequest,
    ): BankAccount

    suspend fun editBankAccount(
        userId: UUID,
        id: UUID,
        editBankAccount: EditBankAccountRequest,
    ): BankAccount?

    suspend fun deleteBankAccount(
        userId: UUID,
        id: UUID,
    ): Boolean
}
