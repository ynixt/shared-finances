package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.transaction.NewTransactionDto
import com.ynixt.sharedfinances.model.dto.transaction.TransactionDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface TransactionService {
    fun findOneIncludeGroupAndCategoriesAndBankAndCreditCard(
        id: Long, user: User, groupId: Long?
    ): Transaction?

    fun findAllIncludeGroupAndCategoriesAsTransactionDto(
        user: User,
        groupId: Long?,
        bankAccountId: Long?,
        creditCardId: Long?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        creditCardBillDate: LocalDate? = null,
        categoriesId: List<Long>?,
        pageable: Pageable
    ): Page<TransactionDto>

    fun newTransaction(user: User, newDto: NewTransactionDto): Transaction
    fun editTransaction(user: User, id: Long, editDto: NewTransactionDto): Transaction

    fun delete(user: User, id: Long, groupId: Long?, deleteAllInstallments: Boolean, deleteNextInstallments: Boolean)
}
