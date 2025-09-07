package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.Group
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupRepository {
    fun findAllByUserIdOrderByName(userId: UUID): Flux<Group>

    fun findOneByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): Mono<Group>

    fun save(group: Group): Mono<Group>
}
