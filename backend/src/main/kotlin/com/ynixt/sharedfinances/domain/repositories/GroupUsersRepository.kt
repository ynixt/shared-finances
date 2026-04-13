package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupUsersRepository {
    fun countByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long>

    fun findOneByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<GroupUserEntity>

    fun save(groupUser: GroupUserEntity): Mono<GroupUserEntity>

    fun findAllMembers(groupId: UUID): Flux<GroupUserEntity>

    fun findAllOptedInUserIds(groupId: UUID): Flux<UUID>

    fun updateRole(
        userId: UUID,
        groupId: UUID,
        newRole: UserGroupRole,
    ): Mono<Long>

    fun updatePlanningSimulatorOptIn(
        userId: UUID,
        groupId: UUID,
        allowPlanningSimulator: Boolean,
    ): Mono<Long>
}
