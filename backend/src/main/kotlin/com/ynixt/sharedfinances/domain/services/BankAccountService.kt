package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import com.ynixt.sharedfinances.domain.models.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.NewBankAccountRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.util.UUID

interface BankAccountService {
    fun findAllBanks(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<BankAccount>>

    fun newBankAccount(
        userId: UUID,
        newBankAccountRequest: NewBankAccountRequest,
    ): Mono<BankAccount>

    fun editBankAccount(
        userId: UUID,
        id: UUID,
        editBankAccount: EditBankAccountRequest,
    ): Mono<BankAccount?>

    fun deleteBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean>
}
