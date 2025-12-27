package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import reactor.core.publisher.Flux
import java.util.UUID

abstract class EntityServiceImpl<E : Any, D : Any> {
    protected abstract val repository: EntityRepository<E>

    fun findAllByIdIn(ids: Collection<UUID>): Flux<D> {
        if (ids.isEmpty()) return Flux.empty()

        return repository.findAllByIdIn(ids).map { convert(it) }
    }

    open fun convert(entity: E): D = entity as D
}
