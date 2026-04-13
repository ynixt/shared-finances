package com.ynixt.sharedfinances.scenarios.support

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.math.BigDecimal
import java.util.UUID

internal class ScenarioGroupService : GroupService {
    private data class MemberScope(
        val role: UserGroupRole,
        val permissions: Set<GroupPermissions>,
        val associatedItemIds: Set<UUID>,
        val allowPlanningSimulator: Boolean,
    )

    private data class GroupState(
        val id: UUID,
        var name: String,
        val members: MutableMap<UUID, MemberScope> = mutableMapOf(),
    )

    private val groups = mutableMapOf<UUID, GroupState>()

    fun createGroup(
        name: String = "Scenario Group",
        id: UUID = UUID.randomUUID(),
    ): UUID {
        groups[id] = GroupState(id = id, name = name)
        return id
    }

    fun upsertMemberScope(
        groupId: UUID,
        userId: UUID,
        role: UserGroupRole = UserGroupRole.EDITOR,
        permissions: Set<GroupPermissions> = GroupPermissions.entries.toSet(),
        associatedItemIds: Set<UUID> = emptySet(),
        allowPlanningSimulator: Boolean = true,
    ) {
        val group = groups.getOrPut(groupId) { GroupState(id = groupId, name = "Scenario Group") }
        group.members[userId] =
            MemberScope(
                role = role,
                permissions = permissions,
                associatedItemIds = associatedItemIds,
                allowPlanningSimulator = allowPlanningSimulator,
            )
    }

    override suspend fun findAllGroups(userId: UUID): List<GroupWithRole> =
        groups.values.mapNotNull { state ->
            state.members[userId]?.let { member ->
                GroupWithRole(
                    id = state.id,
                    createdAt = null,
                    updatedAt = null,
                    name = state.name,
                    role = member.role,
                    itemsAssociated = null,
                ).also {
                    it.permissions = member.permissions
                }
            }
        }

    override suspend fun findGroup(
        userId: UUID,
        id: UUID,
    ): GroupWithRole? = resolveGroup(userId = userId, id = id, includeAssociatedItems = false)

    override suspend fun findGroupWithAssociatedItems(
        userId: UUID,
        id: UUID,
    ): GroupWithRole? = resolveGroup(userId = userId, id = id, includeAssociatedItems = true)

    override suspend fun editGroup(
        userId: UUID,
        id: UUID,
        request: EditGroupRequest,
    ): GroupWithRole? {
        val state = groups[id] ?: return null
        if (!state.members.containsKey(userId)) return null

        state.name = request.name
        return resolveGroup(userId = userId, id = id, includeAssociatedItems = false)
    }

    override suspend fun deleteGroup(
        userId: UUID,
        id: UUID,
    ): Boolean {
        val state = groups[id] ?: return false
        if (!state.members.containsKey(userId)) return false

        groups.remove(id)
        return true
    }

    override suspend fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): GroupEntity {
        val id = createGroup(name = newGroupRequest.name)
        upsertMemberScope(groupId = id, userId = userId)
        return GroupEntity(name = newGroupRequest.name).also { it.id = id }
    }

    override suspend fun findAllMembers(
        userId: UUID,
        id: UUID,
    ): List<GroupUserEntity> {
        val state = groups[id] ?: return emptyList()
        if (!state.members.containsKey(userId)) return emptyList()

        return state.members.entries.map { (memberId, memberScope) ->
            GroupUserEntity(
                groupId = id,
                userId = memberId,
                role = memberScope.role,
                allowPlanningSimulator = memberScope.allowPlanningSimulator,
            )
        }
    }

    override suspend fun updateMemberRole(
        userId: UUID,
        id: UUID,
        memberId: UUID,
        newRole: UserGroupRole,
    ): Boolean {
        val state = groups[id] ?: return false
        val current = state.members[memberId] ?: return false
        if (!state.members.containsKey(userId)) return false

        state.members[memberId] = current.copy(role = newRole)
        return true
    }

    override suspend fun addNewMember(
        userId: UUID,
        id: UUID,
        role: UserGroupRole,
    ) {
        upsertMemberScope(
            groupId = id,
            userId = userId,
            role = role,
            permissions = GroupPermissions.entries.toSet(),
            associatedItemIds = emptySet(),
            allowPlanningSimulator = true,
        )
    }

    override suspend fun updateOwnPlanningSimulatorOptIn(
        userId: UUID,
        id: UUID,
        allowPlanningSimulator: Boolean,
    ): Boolean {
        val state = groups[id] ?: return false
        val current = state.members[userId] ?: return false
        state.members[userId] = current.copy(allowPlanningSimulator = allowPlanningSimulator)
        return true
    }

    override fun findAllByIdIn(ids: Collection<UUID>): Flow<GroupEntity> = emptyFlow()

    private fun resolveGroup(
        userId: UUID,
        id: UUID,
        includeAssociatedItems: Boolean,
    ): GroupWithRole? {
        val state = groups[id] ?: return null
        val member = state.members[userId] ?: return null
        val associatedItems = if (includeAssociatedItems) toAssociatedItems(userId, member.associatedItemIds) else null

        return GroupWithRole(
            id = state.id,
            createdAt = null,
            updatedAt = null,
            name = state.name,
            role = member.role,
            itemsAssociated = associatedItems,
        ).also {
            it.permissions = member.permissions
        }
    }

    private fun toAssociatedItems(
        userId: UUID,
        associatedItemIds: Set<UUID>,
    ): List<WalletItem> =
        associatedItemIds.map { walletItemId ->
            BankAccount(
                name = "Associated Item",
                enabled = true,
                userId = userId,
                currency = "BRL",
                balance = BigDecimal.ZERO,
            ).also {
                it.id = walletItemId
            }
        }
}
