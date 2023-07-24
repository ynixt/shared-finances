package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.TransactionCategory
import com.ynixt.sharedfinances.entity.User

interface TransactionCategoryService {
    fun findAll(user: User, groupId: Long? = null): List<TransactionCategory>
}