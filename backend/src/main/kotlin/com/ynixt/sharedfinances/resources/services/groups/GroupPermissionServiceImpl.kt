package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupPermissionServiceImpl(
    private val groupUserRepository: GroupUsersRepository,
) : GroupPermissionService {
    override suspend fun hasPermission(
        userId: UUID,
        groupId: UUID,
        permission: GroupPermissions?,
    ): Boolean =
        if (permission == null) {
            groupUserRepository
                .countByGroupIdAndUserId(
                    userId = userId,
                    groupId = groupId,
                ).awaitSingle()
                .let { it > 0 }
        } else {
            groupUserRepository
                .findOneByGroupIdAndUserId(
                    userId = userId,
                    groupId = groupId,
                ).awaitSingleOrNull()
                ?.let { getAllPermissionsForRole(it.role).contains(permission) }
                ?: false
        }

    override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> =
        when (role) {
            UserGroupRole.ADMIN -> GroupPermissions.entries.toSet()
            UserGroupRole.EDITOR ->
                setOf(
                    GroupPermissions.SEND_ENTRIES,
                    GroupPermissions.ADD_BANK_ACCOUNT,
                    GroupPermissions.MANAGE_GOALS,
                )

            else -> emptySet()
        }
}
