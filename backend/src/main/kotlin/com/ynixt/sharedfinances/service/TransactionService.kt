package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.transaction.NewTransactionDto
import com.ynixt.sharedfinances.model.dto.transaction.TransactionDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface TransactionService {
    fun findAllByIdIncludeGroupAndCategoryAsTransactionDto(
        user: User,
        groupId: Long?,
        bankAccountId: Long?,
        creditCardId: Long?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        pageable: Pageable
    ): Page<TransactionDto>

    fun newTransaction(user: User, newDto: NewTransactionDto): Transaction
}
