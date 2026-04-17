package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupRepository : EntityRepository<GroupEntity> {
    fun findAllByUserIdOrderByName(userId: UUID): Flux<GroupWithRole>

    fun findOneByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole>

    fun searchByUserIdAndNameContainingIgnoreCase(
        userId: UUID,
        name: String,
    ): Flux<GroupWithRole>

    fun countByUserIdAndNameContainingIgnoreCase(
        userId: UUID,
        name: String,
    ): Mono<Long>

    fun edit(
        id: UUID,
        newName: String,
    ): Mono<Long>
}
