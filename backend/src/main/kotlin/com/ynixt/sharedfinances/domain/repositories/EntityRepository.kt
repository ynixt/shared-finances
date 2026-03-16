package com.ynixt.sharedfinances.domain.repositories

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface EntityRepository<T : Any> {
    fun findById(id: UUID): Mono<T>

    fun deleteById(id: UUID): Mono<Long>

    fun existsById(id: UUID): Mono<Boolean>

    fun <S : T> save(entity: S): Mono<S>

    fun <S : T> saveAll(entity: Iterable<S>): Flux<S>

    fun findAllByIdIn(id: Collection<UUID>): Flux<T>
}
