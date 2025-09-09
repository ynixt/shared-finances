package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.entities.GroupUser
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupService {
    fun findAllGroups(userId: UUID): Mono<List<GroupWithRole>>

    fun findGroup(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole>

    fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): Mono<Group>

    fun findAllMembers(
        userId: UUID,
        id: UUID,
    ): Mono<List<GroupUser>>

    fun updateMemberRole(
        userId: UUID,
        id: UUID,
        memberId: UUID,
        newRole: UserGroupRole,
    ): Mono<Boolean>
}
