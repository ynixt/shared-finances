package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto
import com.ynixt.sharedfinances.model.dto.group.GroupSummaryByUserDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
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
        creditCardBillDate: LocalDate?,
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
                (COALESCE(sum(t.value) * -1, 0)),
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
                    COALESCE(sum(t.value), 0),
                    COALESCE(sum(t.value * -1) FILTER (WHERE t.value < 0), 0),
                    COALESCE(sum(t.value) FILTER (WHERE t.value > 0), 0)
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

    @Query(
        """
          select new com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto(
                    to_char(bd.billDate, 'YYYY-MM'),
                    COALESCE(sum(t.value), 0),
                    COALESCE(sum(t.value * -1) FILTER (WHERE t.value < 0), 0),
                    COALESCE(sum(t.value) FILTER (WHERE t.value > 0), 0)
                )
                from Transaction t
                join t.creditCard c
                join c.billDates bd
                where
                    t.userId = :userId
                    and t.creditCardId = :creditCardId
                    and bd.billDate >= :minCreditCardBillDate
                    and bd.billDate <= :maxCreditCardBillDate
                group by 1
    """
    )
    fun findAllByCreditCardIdGroupedByDate(
        userId: Long,
        creditCardId: Long,
        minCreditCardBillDate: LocalDate,
        maxCreditCardBillDate: LocalDate,
    ): List<TransactionValuesAndDateDto>

    @Query(
        """
          select new com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto(
                    to_char(t.date, 'YYYY-MM'),
                    COALESCE(sum(t.value), 0),
                    COALESCE(sum(t.value * -1) FILTER (WHERE t.value < 0), 0),
                    COALESCE(sum(t.value) FILTER (WHERE t.value > 0), 0)
                )
                from Transaction t
                where
                    t.userId = :userId
                    and t.groupId = :groupId
                    and t.date >= :minDate
                    and t.date <= :maxDate
                group by 1
    """
    )
    fun findAllByGroupIdGroupedByDate(
        userId: Long,
        groupId: Long,
        minDate: LocalDate,
        maxDate: LocalDate,
    ): List<TransactionValuesAndDateDto>

    @Query(
        """
        from Transaction t
        left join fetch t.group g
        left join fetch g.users gu
        where t.installmentId = :installmentId
    """
    )
    fun findAllByInstallmentIdIncludeGroup(installmentId: String): List<Transaction>

    @Modifying
    fun deleteAllByInstallmentId(installmentId: String)

    @Query(
        """
        from Transaction t
        left join fetch t.group g
        left join fetch g.users gu
        where
            t.installmentId = :installmentId
            and t.installment >= :minInstallment
    """
    )
    fun findAllByInstallmentIdAndInstallmentGreaterThanEqualIncludeGroup(
        installmentId: String,
        minInstallment: Int
    ): List<Transaction>

    @Modifying
    fun deleteAllByInstallmentIdAndInstallmentGreaterThanEqual(installmentId: String, minInstallment: Int)
}
