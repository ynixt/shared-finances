package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateAndUserNameDto
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
        categoriesId: List<Long>?
    ): BankAccountSummaryDto

    fun getCreditCardSummary(
        userId: Long, creditCardId: Long?, maxCreditCardBillDate: LocalDate?, categoriesId: List<Long>?
    ): CreditCardSummaryDto


    fun findAllIncludeGroupAndCategoriesAndBankAndCreditCard(
        userId: Long?,
        groupId: Long?,
        bankAccountId: Long?,
        creditCardId: Long?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        creditCardBillDate: LocalDate?,
        categoriesId: List<Long>?,
        pageable: Pageable
    ): Page<Transaction>

    fun findOneIncludeGroupAndCategoriesAndBankAndCreditCard(
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
          select new com.ynixt.sharedfinances.model.dto.group.GroupSummaryByUserDto(
                (COALESCE(sum(t.value) * -1, 0)),
                t.user.id
            )
            from Transaction t
            join t.group g
            where g.id = :groupId
            and t.date >= :minDate
            and t.date <= :maxDate
            and exists (
                select internal_t from Transaction internal_t
                join t.categories internal_c
                where
                    internal_t.id = t.id
                    and internal_c.id in :categoriesId
            )
             group by t.user.id
    """
    )
    fun getGroupSummaryByUserAndCategory(
        groupId: Long, minDate: LocalDate, maxDate: LocalDate, categoriesId: List<Long>
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
                    and t.date <= :maxDate
                group by 1
    """
    )
    fun findAllByBankAccountIdGroupedByDate(
        userId: Long,
        bankAccountId: Long,
        maxDate: LocalDate
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
                    and t.bankAccountId = :bankAccountId
                    and t.date <= :maxDate
                    and exists (
                        select internal_t from Transaction internal_t
                        join t.categories internal_c
                        where
                            internal_t.id = t.id
                            and internal_c.id in :categoriesId
                    )
                group by 1
    """
    )
    fun findAllByBankAccountIdAndCategoriesGroupedByDate(
        userId: Long,
        bankAccountId: Long,
        maxDate: LocalDate,
        categoriesId: List<Long>
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
                    and exists (
                        select internal_t from Transaction internal_t
                        join t.categories internal_c
                        where
                            internal_t.id = t.id
                            and internal_c.id in :categoriesId
                    )
                group by 1
    """
    )
    fun findAllByCreditCardIdAndCategoriesGroupedByDate(
        userId: Long,
        creditCardId: Long,
        minCreditCardBillDate: LocalDate,
        maxCreditCardBillDate: LocalDate,
        categoriesId: List<Long>
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
                join t.user u
                where
                    t.groupId = :groupId
                    and t.date >= :minDate
                    and t.date <= :maxDate
                group by 1
    """
    )
    fun findAllByGroupIdGroupedByDate(
        groupId: Long,
        minDate: LocalDate,
        maxDate: LocalDate,
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
                join t.user u
                where
                    t.groupId = :groupId
                    and t.date >= :minDate
                    and t.date <= :maxDate
                    and exists (
                        select internal_t from Transaction internal_t
                        join t.categories internal_c
                        where
                            internal_t.id = t.id
                            and internal_c.id in :categoriesId
                    )
                group by 1
    """
    )
    fun findAllByGroupIdAndCategoriesGroupedByDate(
        groupId: Long,
        minDate: LocalDate,
        maxDate: LocalDate,
        categoriesId: List<Long>
    ): List<TransactionValuesAndDateDto>

    @Query(
        """
          select new com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateAndUserNameDto(
                    to_char(t.date, 'YYYY-MM'),
                    COALESCE(sum(t.value), 0),
                    COALESCE(sum(t.value * -1) FILTER (WHERE t.value < 0), 0),
                    COALESCE(sum(t.value) FILTER (WHERE t.value > 0), 0),
                    u.id
                )
                from Transaction t
                join t.user u
                where
                    t.groupId = :groupId
                    and t.date >= :minDate
                    and t.date <= :maxDate
                group by 1, u.id
    """
    )
    fun findAllByGroupIdGroupedByDateAndUser(
        groupId: Long,
        minDate: LocalDate,
        maxDate: LocalDate,
    ): List<TransactionValuesAndDateAndUserNameDto>

    @Query(
        """
          select new com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateAndUserNameDto(
                    to_char(t.date, 'YYYY-MM'),
                    COALESCE(sum(t.value), 0),
                    COALESCE(sum(t.value * -1) FILTER (WHERE t.value < 0), 0),
                    COALESCE(sum(t.value) FILTER (WHERE t.value > 0), 0),
                    u.id
                )
                from Transaction t
                join t.user u
                where
                    t.groupId = :groupId
                    and t.date >= :minDate
                    and t.date <= :maxDate
                    and exists (
                        select internal_t from Transaction internal_t
                        join t.categories internal_c
                        where
                            internal_t.id = t.id
                            and internal_c.id in :categoriesId
                    )
                group by 1, u.id
    """
    )
    fun findAllByGroupIdAndCategoriesGroupedByDateAndUser(
        groupId: Long,
        minDate: LocalDate,
        maxDate: LocalDate,
        categoriesId: List<Long>
    ): List<TransactionValuesAndDateAndUserNameDto>

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
