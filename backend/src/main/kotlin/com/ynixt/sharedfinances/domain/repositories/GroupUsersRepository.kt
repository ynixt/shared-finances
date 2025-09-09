package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.GroupUser
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupUsersRepository {
    fun countByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long>

    fun countByGroupIdAndUserIdAndRole(
        groupId: UUID,
        userId: UUID,
        role: UserGroupRole,
    ): Mono<Long>

    fun save(groupUser: GroupUser): Mono<GroupUser>

    fun findAllMembers(groupId: UUID): Flux<GroupUser>

    fun updateRole(
        userId: UUID,
        groupId: UUID,
        newRole: UserGroupRole,
    ): Mono<Long>
}
