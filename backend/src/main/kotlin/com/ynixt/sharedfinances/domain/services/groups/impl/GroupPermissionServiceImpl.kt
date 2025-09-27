package com.ynixt.sharedfinances.domain.services.groups.impl

import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupPermissionServiceImpl(
    private val groupUserRepository: GroupUsersRepository,
) : GroupPermissionService {
    override fun hasPermission(
        userId: UUID,
        groupId: UUID,
        permission: GroupPermissions?,
    ): Mono<Boolean> =
        if (permission == null) {
            groupUserRepository
                .countByGroupIdAndUserId(
                    userId = userId,
                    groupId = groupId,
                ).map { it > 0 }
        } else {
            groupUserRepository
                .findOneByGroupIdAndUserId(
                    userId = userId,
                    groupId = groupId,
                ).map { getAllPermissionsForRole(it.role).contains(permission) }
                .switchIfEmpty(Mono.just(false))
        }

    override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> =
        when (role) {
            UserGroupRole.ADMIN -> GroupPermissions.entries.toSet()
            UserGroupRole.EDITOR ->
                setOf(
                    GroupPermissions.SEND_ENTRIES,
                    GroupPermissions.ADD_BANK_ACCOUNT,
                )

            else -> emptySet()
        }
}
