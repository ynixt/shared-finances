package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.GroupTransactionCategory
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.entity.UserTransactionCategory
import com.ynixt.sharedfinances.model.dto.transactioncategory.*

interface TransactionCategoryService {
    fun findAllUserCategories(user: User): List<UserTransactionCategory>
    fun findAllUserCategoriesAsUserTransactionCategoryDto(user: User): List<UserTransactionCategoryDto>
    fun findAllGroupCategoriesAsGroupTransactionCategoryDto(
        user: User, groupId: Long
    ): List<GroupTransactionCategoryDto>

    fun findAllGroupCategories(user: User, groupId: Long): List<GroupTransactionCategory>
    fun newUserCategory(user: User, newDto: NewUserTransactionCategoryDto): UserTransactionCategory
    fun updateUserCategory(user: User, id: Long, updateDto: UpdateUserTransactionCategoryDto): UserTransactionCategory
    fun getOneUserCategory(user: User, id: Long): UserTransactionCategory?
    fun deleteOneUserCategory(user: User, id: Long)
    fun newGroupCategory(user: User, newDto: NewGroupTransactionCategoryDto): GroupTransactionCategory
    fun updateGroupCategory(
        user: User, id: Long, updateDto: UpdateGroupTransactionCategoryDto
    ): GroupTransactionCategory

    fun getOneGroupCategory(user: User, id: Long): GroupTransactionCategory?

    fun deleteOneGroupCategory(user: User, id: Long)
}
