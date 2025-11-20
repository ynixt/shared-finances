package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
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

    fun findGroupWithAssociatedItems(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole>

    fun editGroup(
        userId: UUID,
        id: UUID,
        request: EditGroupRequest,
    ): Mono<GroupWithRole>

    fun deleteGroup(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean>

    fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): Mono<GroupEntity>

    fun findAllMembers(
        userId: UUID,
        id: UUID,
    ): Mono<List<GroupUserEntity>>

    fun updateMemberRole(
        userId: UUID,
        id: UUID,
        memberId: UUID,
        newRole: UserGroupRole,
    ): Mono<Boolean>

    fun addNewMember(
        userId: UUID,
        id: UUID,
        role: UserGroupRole = UserGroupRole.VIEWER,
    ): Mono<Unit>
}
