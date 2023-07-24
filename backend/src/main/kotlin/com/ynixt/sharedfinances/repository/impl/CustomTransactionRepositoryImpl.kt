package com.ynixt.sharedfinances.repository.impl

import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto
import com.ynixt.sharedfinances.repository.CustomTransactionRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.PersistenceContext
import java.math.BigDecimal
import java.time.ZonedDateTime


class CustomTransactionRepositoryImpl : CustomTransactionRepository {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun getBankAccountSummary(
        userId: Long, bankAccountId: Long?, maxDate: ZonedDateTime?
    ): BankAccountSummaryDto {
        var hql = """
            select new com.ynixt.sharedfinances.dto.bankAccount.BankAccountSummaryDto(
                sum(t.value),
                sum(t.value) FILTER (WHERE t.type <> "Transfer" AND t.value < 0),
                sum(t.value) FILTER (WHERE t.type <> "Transfer" AND t.value > 0)
            )
            from Transaction t
            where t.user.id = :userId
        """.trimIndent()

        if (bankAccountId != null) {
            hql += " and t.bankAccount.id = :bankAccountId"
        }

        if (maxDate != null) {
            hql += " and t.date <= :maxDate"
        }

        hql += """
            group by t.user
        """

        val query = entityManager.createQuery(hql)

        query.setParameter("userId", userId)
        if (bankAccountId != null) {
            query.setParameter("bankAccountId", bankAccountId)
        }
        if (maxDate != null) {
            query.setParameter("maxDate", maxDate)
        }

        return try {
            query.singleResult as BankAccountSummaryDto
        } catch (ex: NoResultException) {
            BankAccountSummaryDto(
                balance = BigDecimal.ZERO, expenses = BigDecimal.ZERO, revenues = BigDecimal.ZERO
            )
        }
    }

    override fun getCreditCardSummary(
        userId: Long, creditCardId: Long?, maxCreditCardBillDate: ZonedDateTime?
    ): CreditCardSummaryDto {
        var hql = """
                    select new com.ynixt.sharedfinances.dto.creditcard.CreditCardSummaryDto(
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

        val query = entityManager.createQuery(hql)

        query.setParameter("userId", userId)
        if (creditCardId != null) {
            query.setParameter("creditCardId", creditCardId)
        }
        if (maxCreditCardBillDate != null) {
            query.setParameter("maxCreditCardBillDate", maxCreditCardBillDate)
        }

        return try {
            query.singleResult as CreditCardSummaryDto
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

}
