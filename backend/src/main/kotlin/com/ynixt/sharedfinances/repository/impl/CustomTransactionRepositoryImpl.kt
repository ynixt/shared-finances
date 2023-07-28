package com.ynixt.sharedfinances.repository.impl

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto
import com.ynixt.sharedfinances.repository.CustomTransactionRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDate


class CustomTransactionRepositoryImpl : CustomTransactionRepository {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun getBankAccountSummary(
        userId: Long, bankAccountId: Long?, maxDate: LocalDate?
    ): BankAccountSummaryDto {
        var hql = """
            select new com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto(
                sum(t.value),
                (sum(t.value) FILTER (WHERE t.value < 0))  * -1,
                sum(t.value) FILTER (WHERE t.value > 0)
            )
            from Transaction t
            where t.user.id = :userId
        """.trimIndent()

        if (bankAccountId != null) {
            hql += " and t.bankAccount.id = :bankAccountId"
        } else {
            hql += " and t.bankAccount.id is not null"
        }

        if (maxDate != null) {
            hql += " and t.date <= :maxDate"
        }

        hql += """
            group by t.user
        """

        val query = entityManager.createQuery(hql, BankAccountSummaryDto::class.java)

        query.setParameter("userId", userId)
        if (bankAccountId != null) {
            query.setParameter("bankAccountId", bankAccountId)
        }
        if (maxDate != null) {
            query.setParameter("maxDate", maxDate)
        }

        return try {
            query.singleResult
        } catch (ex: NoResultException) {
            BankAccountSummaryDto(
                balance = BigDecimal.ZERO, expenses = BigDecimal.ZERO, revenues = BigDecimal.ZERO
            )
        }
    }

    override fun getCreditCardSummary(
        userId: Long, creditCardId: Long?, maxCreditCardBillDate: LocalDate?
    ): CreditCardSummaryDto {
        var hql = """
                    select new com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto(
                        sum(t.value),
                        sum(t.value) FILTER (WHERE t.type = "CreditCard"),
                        sum(t.value) FILTER (WHERE t.type = "CreditCardBillPayment"),
                        sum(t.value) FILTER (WHERE t.type = "CreditCardBillPayment" and bd.billDate = :maxCreditCardBillDate),
                        sum(t.value) FILTER (WHERE t.type = "CreditCard" and bd.billDate = :maxCreditCardBillDate)
                    )
                    from Transaction t
                    join t.creditCard c
                    join c.billDates bd
        """.trimIndent()

        if (creditCardId != null) {
            hql += " and c.id = :creditCardId"
        }

        if (maxCreditCardBillDate != null) {
            hql += " and bd.billDate <= :maxCreditCardBillDate"
        }

        hql += """
            group by t.user
        """

        val query = entityManager.createQuery(hql, CreditCardSummaryDto::class.java)

        query.setParameter("userId", userId)
        if (creditCardId != null) {
            query.setParameter("creditCardId", creditCardId)
        }
        if (maxCreditCardBillDate != null) {
            query.setParameter("maxCreditCardBillDate", maxCreditCardBillDate)
        }

        return try {
            query.singleResult
        } catch (ex: NoResultException) {
            CreditCardSummaryDto(
                bill = BigDecimal.ZERO,
                expenses = BigDecimal.ZERO,
                payments = BigDecimal.ZERO,
                paymentsOfThisBill = BigDecimal.ZERO,
                expensesOfThisBill = BigDecimal.ZERO,
            )
        }
    }

    override fun findAllByIdIncludeGroupAndCategory(
        userId: Long?,
        groupId: Long?,
        bankAccountId: Long?,
        creditCardId: Long?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        pageable: Pageable
    ): Page<Transaction> {
        var hql = """
           from Transaction t
           left join fetch t.group g
           left join fetch t.category c
       """.trimIndent()

        var countHql = "select count(1) from Transaction t"

        if (userId != null || groupId != null || bankAccountId != null || creditCardId != null || minDate != null || maxDate != null) {
            hql += " where"
            countHql += " where"
        }

        if (userId != null) {
            hql += " and t.userId = :userId"
            countHql += " and t.userId = :userId"
        }

        if (groupId != null) {
            hql += " and t.groupId = :groupId"
            countHql += " and t.groupId = :groupId"
        }

        if (bankAccountId != null) {
            hql += " and t.bankAccountId = :bankAccountId"
            countHql += " and t.bankAccountId = :bankAccountId"
        }

        if (creditCardId != null) {
            hql += " and t.creditCardId = :creditCardId"
            countHql += " and t.creditCardId = :creditCardId"
        }

        if (minDate != null) {
            hql += " and t.date >= :minDate"
            countHql += " and t.date >= :minDate"
        }

        if (maxDate != null) {
            hql += " and t.date < :maxDate"
            countHql += " and t.date < :maxDate"
        }

        hql = hql.replace("where and", "where")
        countHql = countHql.replace("where and", "where")

        val countQuery = entityManager.createQuery(countHql, Long::class.java)
        val query = entityManager.createQuery(hql, Transaction::class.java)

        if (userId != null) {
            query.setParameter("userId", userId)
            countQuery.setParameter("userId", userId)
        }

        if (groupId != null) {
            query.setParameter("groupId", groupId)
            countQuery.setParameter("groupId", groupId)
        }

        if (bankAccountId != null) {
            query.setParameter("bankAccountId", bankAccountId)
            countQuery.setParameter("bankAccountId", bankAccountId)
        }

        if (creditCardId != null) {
            query.setParameter("creditCardId", creditCardId)
            countQuery.setParameter("creditCardId", creditCardId)
        }

        if (minDate != null) {
            query.setParameter("minDate", minDate)
            countQuery.setParameter("minDate", minDate)
        }

        if (maxDate != null) {
            query.setParameter("maxDate", maxDate)
            countQuery.setParameter("maxDate", maxDate)
        }

        val count = countQuery.singleResult

        if (count == 0L) {
            return PageImpl(listOf(), pageable, 0)
        }

        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize

        return PageImpl(query.resultList, pageable, count);
    }
}
