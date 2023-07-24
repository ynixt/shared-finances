package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.TransactionCategory
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.repository.GroupTransactionCategoryRepository
import com.ynixt.sharedfinances.repository.UserTransactionCategoryRepository
import com.ynixt.sharedfinances.service.TransactionCategoryService
import org.springframework.stereotype.Service

@Service
class TransactionCategoryServiceImpl(
    private val userTransactionCategoryRepository: UserTransactionCategoryRepository,
    private val groupTransactionCategoryRepository: GroupTransactionCategoryRepository
) : TransactionCategoryService {
    override fun findAll(user: User, groupId: Long?): List<TransactionCategory> {
        return if (groupId == null) {
            userTransactionCategoryRepository.findAllByUserId(user.id!!)
        } else {
            groupTransactionCategoryRepository.findAllByUserIdAndGroupId(user.id!!, groupId)
        }
    }
}