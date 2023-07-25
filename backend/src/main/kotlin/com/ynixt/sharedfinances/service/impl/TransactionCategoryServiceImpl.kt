package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.Group
import com.ynixt.sharedfinances.entity.GroupTransactionCategory
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.entity.UserTransactionCategory
import com.ynixt.sharedfinances.mapper.TransactionCategoryMapper
import com.ynixt.sharedfinances.model.dto.transactioncategory.*
import com.ynixt.sharedfinances.model.exceptions.SFException
import com.ynixt.sharedfinances.repository.GroupTransactionCategoryRepository
import com.ynixt.sharedfinances.repository.UserTransactionCategoryRepository
import com.ynixt.sharedfinances.service.TransactionCategoryService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class TransactionCategoryServiceImpl(
    private val userTransactionCategoryRepository: UserTransactionCategoryRepository,
    private val groupTransactionCategoryRepository: GroupTransactionCategoryRepository,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val transactionCategoryMapper: TransactionCategoryMapper,
    private val entityManager: EntityManager
) : TransactionCategoryService {
    override fun findAllUserCategories(user: User): List<UserTransactionCategory> {
        return userTransactionCategoryRepository.findAllByUserId(user.id!!)
    }

    override fun findAllUserCategoriesAsUserTransactionCategoryDto(user: User): List<UserTransactionCategoryDto> {
        return transactionCategoryMapper.toDtoUserList(userTransactionCategoryRepository.findAllByUserId(user.id!!))!!
    }

    override fun findAllGroupCategoriesAsGroupTransactionCategoryDto(
        user: User,
        groupId: Long
    ): List<GroupTransactionCategoryDto> {
        return transactionCategoryMapper.toDtoGroupList(
            groupTransactionCategoryRepository.findAllByUserIdAndGroupId(
                user.id!!,
                groupId
            )
        )!!
    }

    override fun findAllGroupCategories(user: User, groupId: Long): List<GroupTransactionCategory> {
        return groupTransactionCategoryRepository.findAllByUserIdAndGroupId(user.id!!, groupId)
    }

    @Transactional
    override fun newUserCategory(user: User, newDto: NewUserTransactionCategoryDto): UserTransactionCategory {
        var category = UserTransactionCategory(
            user = user,
            name = newDto.name,
            color = newDto.color,
        )

        category = userTransactionCategoryRepository.save(category)

        userCategoryWasUpdated(user)

        return category
    }

    @Transactional
    override fun updateUserCategory(
        user: User,
        id: Long,
        updateDto: UpdateUserTransactionCategoryDto
    ): UserTransactionCategory {
        var category = getOneUserCategory(user, id) ?: throw SFException(
            reason = "User category not found"
        )

        transactionCategoryMapper.updateUser(category, updateDto)
        category = userTransactionCategoryRepository.save(category)

        userCategoryWasUpdated(user)

        return category
    }

    override fun getOneUserCategory(user: User, id: Long): UserTransactionCategory? {
        return userTransactionCategoryRepository.findOneByIdAndUserId(id = id, userId = user.id!!)
    }

    @Transactional
    override fun deleteOneUserCategory(user: User, id: Long) {
        userTransactionCategoryRepository.deleteOneByIdAndUserId(userId = user.id!!, id = id)
        userCategoryWasUpdated(user)
    }

    override fun newGroupCategory(user: User, newDto: NewGroupTransactionCategoryDto): GroupTransactionCategory {
        var category = GroupTransactionCategory(
            group = entityManager.getReference(Group::class.java, newDto.groupId),
            name = newDto.name,
            color = newDto.color,
        ).apply {
            groupId = newDto.groupId
        }

        category = groupTransactionCategoryRepository.save(category)

        groupCategoryWasUpdated(user, category.groupId!!)

        return category
    }

    override fun updateGroupCategory(
        user: User,
        id: Long,
        updateDto: UpdateGroupTransactionCategoryDto
    ): GroupTransactionCategory {
        var category = getOneGroupCategory(user, id) ?: throw SFException(
            reason = "Group category not found"
        )

        transactionCategoryMapper.updateGroup(category, updateDto)
        category = groupTransactionCategoryRepository.save(category)

        groupCategoryWasUpdated(user, category.groupId!!)

        return category
    }

    override fun getOneGroupCategory(user: User, id: Long): GroupTransactionCategory? {
        return groupTransactionCategoryRepository.findOneByUserIdAndGroupId(id = id, userId = user.id!!)
    }

    @Transactional
    override fun deleteOneGroupCategory(user: User, id: Long) {
        val category = groupTransactionCategoryRepository.findOneByUserIdAndGroupId(userId = user.id!!, id = id)

        if (category != null) {
            groupTransactionCategoryRepository.deleteById(id)
            groupCategoryWasUpdated(user, category.groupId!!)
        }
    }

    private fun userCategoryWasUpdated(user: User) {
        simpMessagingTemplate.convertAndSendToUser(
            user.email, "/queue/user-transaction-category", findAllUserCategoriesAsUserTransactionCategoryDto(user)
        )
    }

    private fun groupCategoryWasUpdated(user: User, groupId: Long) {
        simpMessagingTemplate.convertAndSend(
            "/topic/group-transaction-category/$groupId",
            findAllGroupCategoriesAsGroupTransactionCategoryDto(user, groupId)
        )
    }
}
