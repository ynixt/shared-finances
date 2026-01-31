package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import java.util.UUID

interface BankAccountActionEventService {
    suspend fun sendInsertedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    )

    suspend fun sendUpdatedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    )

    suspend fun sendDeletedBankAccount(
        userId: UUID,
        id: UUID,
    )
}
