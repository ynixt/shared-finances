package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupRepository {
    fun findAllByUserIdOrderByName(userId: UUID): Flux<GroupWithRole>

    fun findOneByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole>

    fun save(group: GroupEntity): Mono<GroupEntity>

    fun edit(
        id: UUID,
        newName: String,
    ): Mono<Long>

    fun deleteById(id: UUID): Mono<Long>
}
