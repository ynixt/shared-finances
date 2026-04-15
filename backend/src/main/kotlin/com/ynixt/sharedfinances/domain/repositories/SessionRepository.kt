package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.SessionEntity
import reactor.core.publisher.Mono
import java.util.UUID

interface SessionRepository : EntityRepository<SessionEntity> {
    /** Removes every auth session (and linked refresh tokens) for the given user. */
    fun deleteAllByUserId(userId: UUID): Mono<Long>
}
