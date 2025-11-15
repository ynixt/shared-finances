package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.bankaccount.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.bankaccount.NewBankAccountRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.util.UUID

interface BankAccountService {
    fun findAllBanks(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<BankAccount>>

    fun findBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<BankAccount>

    fun newBankAccount(
        userId: UUID,
        newBankAccountRequest: NewBankAccountRequest,
    ): Mono<BankAccount>

    fun editBankAccount(
        userId: UUID,
        id: UUID,
        editBankAccount: EditBankAccountRequest,
    ): Mono<BankAccount>

    fun deleteBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean>
}
