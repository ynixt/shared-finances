package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import reactor.core.publisher.Flux
import java.util.UUID

abstract class EntityServiceImpl<E, D> {
    protected abstract val repository: EntityRepository<E>

    fun findAllByIdIn(ids: Collection<UUID>): Flux<D> = repository.findAllByIdIn(ids).map { convert(it) }

    open fun convert(entity: E): D = entity as D
}
