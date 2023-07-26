package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto
import com.ynixt.sharedfinances.model.dto.group.GroupSummaryByUserDto
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate

interface CustomTransactionRepository {
    fun getBankAccountSummary(
        userId: Long,
        bankAccountId: Long?,
        maxDate: LocalDate?
    ): BankAccountSummaryDto

    fun getCreditCardSummary(
        userId: Long,
        creditCardId: Long?,
        maxCreditCardBillDate: LocalDate?
    ): CreditCardSummaryDto

    fun getGroupSummaryByUser(
        groupId: Long,
        minDate: LocalDate?,
        maxDate: LocalDate?
    ): List<GroupSummaryByUserDto>
}

interface TransactionRepository : CrudRepository<Transaction, Long>, CustomTransactionRepository {
}
