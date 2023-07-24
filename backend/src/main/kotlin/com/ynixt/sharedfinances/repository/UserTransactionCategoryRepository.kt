package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.UserTransactionCategory
import org.springframework.data.repository.CrudRepository


interface UserTransactionCategoryRepository : CrudRepository<UserTransactionCategory, Long> {
    fun findAllByUserId(userId: Long): List<UserTransactionCategory>
}