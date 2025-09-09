package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
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
        roleNeeded: UserGroupRole?,
    ): Mono<Boolean> =
        if (roleNeeded == null) {
            groupUserRepository
                .countByGroupIdAndUserId(
                    userId = userId,
                    groupId = groupId,
                ).map { it > 0 }
        } else {
            groupUserRepository
                .countByGroupIdAndUserIdAndRole(
                    userId = userId,
                    groupId = groupId,
                    role = roleNeeded,
                ).map { it > 0 }
        }
}
