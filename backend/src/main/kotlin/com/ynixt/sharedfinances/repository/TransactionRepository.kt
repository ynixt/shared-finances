package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto
import org.springframework.data.repository.Repository
import java.time.ZonedDateTime

interface CustomTransactionRepository {
    fun getBankAccountSummary(
        userId: Long,
        bankAccountId: Long?,
        maxDate: ZonedDateTime?
    ): BankAccountSummaryDto

    fun getCreditCardSummary(
        userId: Long,
        creditCardId: Long?,
        maxCreditCardBillDate: ZonedDateTime?
    ): CreditCardSummaryDto
}

interface TransactionRepository : Repository<Transaction, Long>, CustomTransactionRepository {
}
