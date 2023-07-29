package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.Group
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.mapper.GroupMapper
import com.ynixt.sharedfinances.model.dto.group.*
import com.ynixt.sharedfinances.model.exceptions.SFException
import com.ynixt.sharedfinances.repository.GroupRepository
import com.ynixt.sharedfinances.repository.TransactionRepository
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
    private val transactionRepository: TransactionRepository
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

    override fun getOneAsViewDto(user: User, id: Long): GroupViewDto? {
        val group = groupRepository.getOneByIdAndUserIdWithUsers(id = id, userId = user.id!!)
        return groupMapper.toViewDto(group)
    }

    @Transactional
    override fun updateGroup(user: User, id: Long, updateDto: UpdateGroupDto): Group {
        var group = getOne(user, id) ?: throw SFException(
            reason = "Group not found"
        )

        groupMapper.update(group, updateDto)
        group = groupRepository.save(group)
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

        group = groupRepository.save(group)

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
        }

        val groupView = getOneAsViewDto(users[0]!!, groupId)

        if (groupView != null) {
            simpMessagingTemplate.convertAndSend(
                "/topic/group/$groupId", groupView
            )
        } else {
            // TODO: send that group was deleted!
        }
    }

    override fun getGroupSummary(
        user: User, groupId: Long, minDate: LocalDate?, maxDate: LocalDate?
    ): GroupSummaryDto {
        if (!groupRepository.existsOneByIdAndUserId(userId = user.id!!, id = groupId)) {
            return GroupSummaryDto(listOf())
        }

        val expensesOfUsers = transactionRepository.getGroupSummaryByUser(
            groupId = groupId, minDate = minDate ?: LocalDate.now(), maxDate = maxDate ?: LocalDate.now().plusDays(1)
        )

        return GroupSummaryDto(expensesOfUsers)
    }
}