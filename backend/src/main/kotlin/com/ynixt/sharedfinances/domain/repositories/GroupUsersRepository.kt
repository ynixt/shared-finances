package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.GroupUser
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupUsersRepository {
    fun countByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long>

    fun save(groupUser: GroupUser): Mono<GroupUser>

    fun findAllMembers(groupId: UUID): Flux<GroupUser>
}
