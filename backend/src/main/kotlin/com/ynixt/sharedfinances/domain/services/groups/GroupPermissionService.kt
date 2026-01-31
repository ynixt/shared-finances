package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import java.util.UUID

interface GroupPermissionService {
    suspend fun hasPermission(
        userId: UUID,
        groupId: UUID,
        permission: GroupPermissions? = null,
    ): Boolean

    fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions>
}
