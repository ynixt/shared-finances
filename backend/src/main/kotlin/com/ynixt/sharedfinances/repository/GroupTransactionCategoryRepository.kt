package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.GroupTransactionCategory
import org.springframework.data.repository.CrudRepository


interface GroupTransactionCategoryRepository : CrudRepository<GroupTransactionCategory, Long> {
    fun findAllByUserIdAndGroupId(userId: Long, groupId: Long): List<GroupTransactionCategory>
}