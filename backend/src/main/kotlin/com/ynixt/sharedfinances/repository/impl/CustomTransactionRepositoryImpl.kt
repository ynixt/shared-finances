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
        userId: Long, bankAccountId: Long?, maxDate: LocalDate?, categoriesId: List<Long>?
    ): BankAccountSummaryDto {
        var hql = """
            select new com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto(
                COALESCE(sum(t.value), 0),
                COALESCE((sum(t.value) FILTER (WHERE t.value < 0))  * -1, 0),
                COALESCE(sum(t.value) FILTER (WHERE t.value > 0), 0)
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

        if (categoriesId != null) {
            hql += """
                and exists (
                        select internal_t from Transaction internal_t
                        join t.categories internal_c
                        where
                            internal_t.id = t.id
                            and internal_c.id in :categoriesId
                    )
            """
        }

        hql += " group by t.user"

        val query = entityManager.createQuery(hql, BankAccountSummaryDto::class.java)

        query.setParameter("userId", userId)
        if (bankAccountId != null) {
            query.setParameter("bankAccountId", bankAccountId)
        }
        if (maxDate != null) {
            query.setParameter("maxDate", maxDate)
        }
        if (categoriesId != null) {
            query.setParameter("categoriesId", categoriesId)
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
        userId: Long, creditCardId: Long?, maxCreditCardBillDate: LocalDate?, categoriesId: List<Long>?
    ): CreditCardSummaryDto {
        var hql = """
                    select new com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto(
                        COALESCE(sum(t.value), 0),
                        COALESCE(sum(t.value) FILTER (WHERE t.type = "CreditCard"), 0),
                        COALESCE(sum(t.value) FILTER (WHERE t.type = "CreditCardBillPayment"), 0),
                        COALESCE(sum(t.value) FILTER (WHERE t.type = "CreditCardBillPayment" and bd.billDate = :maxCreditCardBillDate), 0),
                        COALESCE(sum(t.value) FILTER (WHERE t.type = "CreditCard" and bd.billDate = :maxCreditCardBillDate), 0)
                    )
                    from Transaction t
                    join t.creditCard c
                    left join t.creditCardBillDate bd
                    where t.userId = :userId
        """.trimIndent()

        if (creditCardId != null) {
            hql += " and c.id = :creditCardId"
        }

        if (maxCreditCardBillDate != null) {
            hql += " and bd.billDate <= :maxCreditCardBillDate"
        }

        if (categoriesId != null) {
            hql += """
                and exists (
                        select internal_t from Transaction internal_t
                        join t.categories internal_c
                        where
                            internal_t.id = t.id
                            and internal_c.id in :categoriesId
                    )
            """
        }

        hql += " group by t.user"

        val query = entityManager.createQuery(hql, CreditCardSummaryDto::class.java)

        query.setParameter("userId", userId)
        if (creditCardId != null) {
            query.setParameter("creditCardId", creditCardId)
        }

        if (maxCreditCardBillDate != null) {
            query.setParameter("maxCreditCardBillDate", maxCreditCardBillDate)
        }

        if (categoriesId != null) {
            query.setParameter("categoriesId", categoriesId)
        }

        return try {
            query.singleResult
        } catch (ex: NoResultException) {
            CreditCardSummaryDto()
        }
    }

    override fun findAllIncludeGroupAndCategoriesAndBankAndCreditCard(
        userId: Long?,
        groupId: Long?,
        bankAccountId: Long?,
        creditCardId: Long?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        creditCardBillDate: LocalDate?,
        categoriesId: List<Long>?,
        pageable: Pageable
    ): Page<Transaction> {
        var hql = """
           select distinct t
           from Transaction t
           join fetch t.user u
           left join fetch t.group g
           left join fetch t.categories c
           left join fetch t.bankAccount b
           left join fetch t.otherSide oc
           left join fetch oc.bankAccount ocb
           left join fetch oc.user ocu
           left join fetch t.creditCard cc
           left join fetch t.creditCardBillDate ccbd
       """.trimIndent()

        var countHql = """
            select count(distinct t.id) from Transaction t
            left join t.creditCard cc
            left join t.creditCardBillDate ccbd
            left join t.categories c
        """.trimIndent()

        if (
            userId != null || groupId != null || bankAccountId != null || creditCardId != null || minDate != null ||
            maxDate != null || creditCardBillDate != null
        ) {
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
            hql += " and (t.creditCardId is null and t.date >= :minDate or t.creditCardId is not null and t.date >= :minDate)"
            countHql += " and t.date >= :minDate"
        }

        if (maxDate != null) {
            hql += " and t.date <= :maxDate"
            countHql += " and t.date <= :maxDate"
        }

        if (creditCardBillDate != null) {
            hql += " and ccbd.billDate = :creditCardBillDate"
            countHql += " and ccbd.billDate = :creditCardBillDate"
        }

        if (categoriesId != null) {
            hql += " and c.id in :categoriesId"
            countHql += " and c.id in :categoriesId"
        }

        hql += " order by t.date desc, t.id desc"

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

        if (creditCardBillDate != null) {
            query.setParameter("creditCardBillDate", creditCardBillDate)
            countQuery.setParameter("creditCardBillDate", creditCardBillDate)
        }

        if (categoriesId != null) {
            query.setParameter("categoriesId", categoriesId)
            countQuery.setParameter("categoriesId", categoriesId)
        }

        val count = countQuery.singleResult

        if (count == 0L) {
            return PageImpl(listOf(), pageable, 0)
        }

        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize

        return PageImpl(query.resultList, pageable, count);
    }

    override fun findOneIncludeGroupAndCategoriesAndBankAndCreditCard(
        id: Long, userId: Long?, groupId: Long?
    ): Transaction? {
        var hql = """
           select distinct t
           from Transaction t
           join fetch t.user u
           left join fetch t.group g
           left join fetch t.creditCard cc
           left join fetch t.bankAccount b
           left join fetch g.users gu
           left join fetch t.categories c
           left join fetch t.otherSide oc
           left join fetch oc.bankAccount ocb
           left join fetch oc.user ocu
           where t.id = :id
       """.trimIndent()

        if (userId != null) {
            hql += " and t.userId = :userId"
        }

        if (groupId != null) {
            hql += " and t.groupId = :groupId"
        }

        val query = entityManager.createQuery(hql, Transaction::class.java)

        query.setParameter("id", id)

        if (userId != null) {
            query.setParameter("userId", userId)
        }

        if (groupId != null) {
            query.setParameter("groupId", groupId)
        }

        return try {
            query.singleResult
        } catch (ex: NoResultException) {
            null
        }
    }

    override fun findOne(
        id: Long, userId: Long?, groupId: Long?
    ): Transaction? {
        var hql = """
           from Transaction t
           where t.id = :id
       """.trimIndent()

        if (userId != null) {
            hql += " and t.userId = :userId"
        }

        if (groupId != null) {
            hql += " and t.groupId = :groupId"
        }

        val query = entityManager.createQuery(hql, Transaction::class.java)

        query.setParameter("id", id)

        if (userId != null) {
            query.setParameter("userId", userId)
        }

        if (groupId != null) {
            query.setParameter("groupId", groupId)
        }

        return try {
            query.singleResult
        } catch (ex: NoResultException) {
            null
        }
    }
}
