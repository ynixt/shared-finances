package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupPermissionService {
    fun hasPermission(
        userId: UUID,
        groupId: UUID,
        roleNeeded: UserGroupRole? = null,
    ): Mono<Boolean>
}
