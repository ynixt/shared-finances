package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupPermissionService {
    fun hasPermission(
        userId: UUID,
        groupId: UUID,
        permission: GroupPermissions? = null,
    ): Mono<Boolean>

    fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions>
}
