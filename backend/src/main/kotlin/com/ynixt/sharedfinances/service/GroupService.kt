package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.Group
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.group.*
import java.time.LocalDate

interface GroupService {
    fun listGroup(user: User): List<Group>
    fun listGroupAsGroupDto(user: User): List<GroupDto>
    fun getOne(user: User, id: Long): Group?
    fun getOneAsViewDto(user: User, id: Long): GroupViewDto?
    fun updateGroup(user: User, id: Long, updateDto: UpdateGroupDto): Group
    fun newGroup(user: User, newDto: NewGroupDto): Group
    fun delete(user: User, id: Long)
    fun userHasPermissionToGroup(user: User, groupId: Long): Boolean
    fun getGroupSummary(user: User, groupId: Long, minDate: LocalDate?, maxDate: LocalDate?): GroupSummaryDto
}
