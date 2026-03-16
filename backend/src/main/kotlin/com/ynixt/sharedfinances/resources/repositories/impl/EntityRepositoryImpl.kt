package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

abstract class EntityRepositoryImpl<R : EntityRepository<E>, E : Any>(
    protected val springDataRepository: R,
) {
    open fun findById(id: UUID): Mono<E> = springDataRepository.findById(id)

    open fun deleteById(id: UUID): Mono<Long> = springDataRepository.deleteById(id)

    open fun existsById(id: UUID): Mono<Boolean> = springDataRepository.existsById(id)

    open fun <S : E> save(entity: S): Mono<S> = springDataRepository.save(entity)

    open fun <S : E> saveAll(entity: Iterable<S>): Flux<S> = springDataRepository.saveAll(entity)

    open fun findAllByIdIn(id: Collection<UUID>): Flux<E> = springDataRepository.findAllByIdIn(id)
}
