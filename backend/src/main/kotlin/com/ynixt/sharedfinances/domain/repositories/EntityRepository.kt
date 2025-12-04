package com.ynixt.sharedfinances.domain.repositories

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface EntityRepository<T> {
    fun findById(id: UUID): Mono<T>

    fun save(entity: T): Mono<T>

    fun findAllByIdIn(id: Collection<UUID>): Flux<T>
}
