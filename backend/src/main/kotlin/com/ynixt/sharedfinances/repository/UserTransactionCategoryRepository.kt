package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.UserTransactionCategory
import org.springframework.data.repository.CrudRepository


interface UserTransactionCategoryRepository : CrudRepository<UserTransactionCategory, Long> {
    fun findOneByIdAndUserId(id: Long, userId: Long): UserTransactionCategory?
    fun findAllByUserId(userId: Long): List<UserTransactionCategory>
    fun deleteOneByIdAndUserId(id: Long, userId: Long)
}
