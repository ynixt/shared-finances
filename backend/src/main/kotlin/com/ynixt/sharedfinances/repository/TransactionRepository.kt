package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto
import com.ynixt.sharedfinances.model.dto.group.GroupSummaryByUserDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate

interface CustomTransactionRepository {
    fun getBankAccountSummary(
        userId: Long,
        bankAccountId: Long?,
        maxDate: LocalDate?,
    ): BankAccountSummaryDto

    fun getCreditCardSummary(
        userId: Long, creditCardId: Long?, maxCreditCardBillDate: LocalDate?
    ): CreditCardSummaryDto


    fun findAllIncludeGroupAndCategory(
        userId: Long?,
        groupId: Long?,
        bankAccountId: Long?,
        creditCardId: Long?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        pageable: Pageable
    ): Page<Transaction>

    fun findOneIncludeGroupAndCategory(
        id: Long,
        userId: Long?,
        groupId: Long?,
    ): Transaction?

    fun findOne(
        id: Long,
        userId: Long?,
        groupId: Long?,
    ): Transaction?
}

interface TransactionRepository : CrudRepository<Transaction, Long>, CustomTransactionRepository {
    fun saveAndFlush(entity: Transaction): Transaction

    @Query(
        """
          select new com.ynixt.sharedfinances.model.dto.group.GroupSummaryByUserDto(
                (sum(t.value) * -1.0),
                t.user.id
            )
            from Transaction t
            join t.group g
            where g.id = :groupId
            and t.date >= :minDate
            and t.date <= :maxDate
             group by t.user.id
    """
    )
    fun getGroupSummaryByUser(
        groupId: Long, minDate: LocalDate, maxDate: LocalDate
    ): List<GroupSummaryByUserDto>

    @Query(
        """
          select new com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto(
                    to_char(t.date, 'YYYY-MM'),
                    sum(t.value),
                    sum(t.value * -1) FILTER (WHERE t.value < 0),
                    sum(t.value) FILTER (WHERE t.value > 0)
                )
                from Transaction t
                where
                    t.userId = :userId
                    and t.bankAccountId = :bankAccountId
                    and t.date >= :minDate
                    and t.date <= :maxDate
                group by 1
    """
    )
    fun findAllByBankAccountIdGroupedByDate(
        userId: Long,
        bankAccountId: Long,
        minDate: LocalDate,
        maxDate: LocalDate,
    ): List<TransactionValuesAndDateDto>
}
