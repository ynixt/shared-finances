package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import java.util.UUID

abstract class EntityServiceImpl<E : Any, D : Any> {
    protected abstract val repository: EntityRepository<E>

    fun findAllByIdIn(ids: Collection<UUID>): Flow<D> {
        if (ids.isEmpty()) return flow {}

        return repository.findAllByIdIn(ids).map { convert(it) }.asFlow()
    }

    open fun convert(entity: E): D = entity as D
}
