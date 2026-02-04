package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

abstract class EntityRepositoryImpl<T : EntityRepository<U>, U : Any>(
    protected val springDataRepository: T,
) {
    open fun findById(id: UUID): Mono<U> = springDataRepository.findById(id)

    open fun deleteById(id: UUID): Mono<Long> = springDataRepository.deleteById(id)

    open fun existsById(id: UUID): Mono<Boolean> = springDataRepository.existsById(id)

    open fun save(entity: U): Mono<U> = springDataRepository.save(entity)

    open fun saveAll(entity: Iterable<U>): Flux<U> = springDataRepository.saveAll(entity)

    open fun findAllByIdIn(id: Collection<UUID>): Flux<U> = springDataRepository.findAllByIdIn(id)
}
