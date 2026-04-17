package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface GroupService {
    suspend fun findAllGroups(userId: UUID): List<GroupWithRole>

    suspend fun searchGroups(
        userId: UUID,
        pageable: Pageable,
        query: String?,
    ): Page<GroupWithRole>

    suspend fun findGroup(
        userId: UUID,
        id: UUID,
    ): GroupWithRole?

    suspend fun findGroupWithAssociatedItems(
        userId: UUID,
        id: UUID,
    ): GroupWithRole?

    suspend fun editGroup(
        userId: UUID,
        id: UUID,
        request: EditGroupRequest,
    ): GroupWithRole?

    suspend fun deleteGroup(
        userId: UUID,
        id: UUID,
    ): Boolean

    suspend fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): GroupEntity

    suspend fun findAllMembers(
        userId: UUID,
        id: UUID,
    ): List<GroupUserEntity>

    suspend fun updateMemberRole(
        userId: UUID,
        id: UUID,
        memberId: UUID,
        newRole: UserGroupRole,
    ): Boolean

    suspend fun addNewMember(
        userId: UUID,
        id: UUID,
        role: UserGroupRole = UserGroupRole.VIEWER,
    )

    suspend fun updateOwnPlanningSimulatorOptIn(
        userId: UUID,
        id: UUID,
        allowPlanningSimulator: Boolean,
    ): Boolean

    fun findAllByIdIn(ids: Collection<UUID>): Flow<GroupEntity>
}
