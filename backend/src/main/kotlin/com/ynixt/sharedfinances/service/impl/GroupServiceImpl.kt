package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.Group
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.mapper.GroupMapper
import com.ynixt.sharedfinances.model.dto.TransactionValuesGroupChartDto
import com.ynixt.sharedfinances.model.dto.group.*
import com.ynixt.sharedfinances.model.exceptions.SFException
import com.ynixt.sharedfinances.model.exceptions.SFExceptionForbidden
import com.ynixt.sharedfinances.repository.GroupRepository
import com.ynixt.sharedfinances.repository.TransactionRepository
import com.ynixt.sharedfinances.repository.UserRepository
import com.ynixt.sharedfinances.service.GroupService
import jakarta.transaction.Transactional
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GroupServiceImpl(
    private val groupRepository: GroupRepository,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val groupMapper: GroupMapper,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) : GroupService {
    override fun listGroup(user: User): List<Group> {
        return groupRepository.getAllByUserId(user.id!!)
    }

    override fun listGroupAsGroupDto(user: User): List<GroupDto> {
        return groupMapper.toDtoList(listGroup(user))!!
    }

    override fun getOne(user: User, id: Long): Group? {
        return groupRepository.getOneByIdAndUserId(id = id, userId = user.id!!)
    }

    override fun getOne(id: Long): Group? {
        return groupRepository.findById(id).orElse(null)
    }

    override fun save(group: Group): Group {
        return groupRepository.save(group)
    }

    override fun getOneAsViewDto(user: User, id: Long): GroupViewDto? {
        if (!userHasPermissionToGroup(user, id)) {
            throw SFExceptionForbidden()
        }

        val group = groupRepository.getOneByIdWithUsers(id)
        return groupMapper.toViewDto(group)
    }

    @Transactional
    override fun updateGroup(user: User, id: Long, updateDto: UpdateGroupDto): Group {
        var group = getOne(user, id) ?: throw SFException(
            reason = "Group not found"
        )

        groupMapper.update(group, updateDto)
        group = save(group)
        groupWasUpdated(id, group.users!!)
        return group
    }

    @Transactional
    override fun newGroup(user: User, newDto: NewGroupDto): Group {
        var group = Group(
            name = newDto.name
        ).apply {
            users = mutableListOf(user)
        }

        group = save(group)

        groupWasUpdated(group.id!!, group.users!!)

        return group
    }

    @Transactional
    override fun delete(user: User, id: Long) {
        var group = getOne(user, id) ?: throw SFException(
            reason = "Group not found"
        )
        val users = group.users!!

        groupRepository.deleteById(group.id!!)

        groupWasUpdated(id, users)
    }

    private fun groupWasUpdated(groupId: Long, users: List<User>) {
        users.forEach {
            simpMessagingTemplate.convertAndSendToUser(
                it.email, "/queue/group", listGroupAsGroupDto(it)
            )

            val groupView = getOneAsViewDto(users[0]!!, groupId)

            if (groupView != null) {
                simpMessagingTemplate.convertAndSendToUser(
                    it.email, "/queue/group/$groupId", groupView
                )
            } else {
                // TODO: send that group was deleted!
            }
        }
    }

    override fun userHasPermissionToGroup(user: User, groupId: Long): Boolean {
        return groupRepository.existsOneByIdAndUserId(userId = user.id!!, id = groupId)
    }

    override fun getGroupSummary(
        user: User, groupId: Long, minDate: LocalDate?, maxDate: LocalDate?, categoriesId: List<Long>?
    ): GroupSummaryDto {
        if (!userHasPermissionToGroup(user, groupId)) {
            return GroupSummaryDto(listOf())
        }

        val expensesOfUsers = if (categoriesId == null) transactionRepository.getGroupSummaryByUser(
            groupId = groupId, minDate = minDate ?: LocalDate.now(), maxDate = maxDate ?: LocalDate.now().plusDays(1)
        ) else transactionRepository.getGroupSummaryByUserAndCategory(
            groupId = groupId,
            minDate = minDate ?: LocalDate.now(),
            maxDate = maxDate ?: LocalDate.now().plusDays(1),
            categoriesId = categoriesId
        )

        return GroupSummaryDto(expensesOfUsers)
    }

    override fun getChartByGroupId(
        user: User, groupId: Long, minDate: LocalDate?, maxDate: LocalDate?, categoriesId: List<Long>?
    ): TransactionValuesGroupChartDto {
        if (!userHasPermissionToGroup(user, groupId)) {
            throw SFExceptionForbidden()
        }

        val byUser = if (categoriesId == null) transactionRepository.findAllByGroupIdGroupedByDateAndUser(
            groupId = groupId, minDate = minDate ?: LocalDate.now(), maxDate = maxDate ?: LocalDate.now().plusDays(1)
        ) else transactionRepository.findAllByGroupIdAndCategoriesGroupedByDateAndUser(
            groupId = groupId,
            minDate = minDate ?: LocalDate.now(),
            maxDate = maxDate ?: LocalDate.now().plusDays(1),
            categoriesId = categoriesId
        )

        val all = if (categoriesId == null) transactionRepository.findAllByGroupIdGroupedByDate(
            groupId = groupId, minDate = minDate ?: LocalDate.now(), maxDate = maxDate ?: LocalDate.now().plusDays(1)
        ) else transactionRepository.findAllByGroupIdAndCategoriesGroupedByDate(
            groupId = groupId,
            minDate = minDate ?: LocalDate.now(),
            maxDate = maxDate ?: LocalDate.now().plusDays(1),
            categoriesId = categoriesId
        )

        return TransactionValuesGroupChartDto(
            values = all, allValuesByUser = byUser
        )
    }

    override fun listAllWithUsers(user: User): List<Group> {
        // TODO: improve
        val groups = groupRepository.findAllByUserIdIncludeUsers(user.id!!)
        val usersIds = groups.map { it.users!! }.flatten().map { it.id!! }
        val usersById = userRepository.findAllIncludeCreditCardAndBankAccount(usersIds).associateBy { it.id!! }

        groups.forEach { g ->
            g.users = g.users!!.map {
                usersById[it.id!!]!!
            }.toMutableList()
        }

        return groups
    }
}
