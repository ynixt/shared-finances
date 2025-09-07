package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import reactor.core.publisher.Mono
import java.util.UUID

interface BankAccountActionEventService {
    fun sendInsertedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    ): Mono<Long>

    fun sendUpdatedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    ): Mono<Long>

    fun sendDeletedBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<Long>
}
