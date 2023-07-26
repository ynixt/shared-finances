package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.transaction.TransactionDto

interface TransactionService {
    fun newTransaction(user: User, newDto: TransactionDto): Transaction
}
