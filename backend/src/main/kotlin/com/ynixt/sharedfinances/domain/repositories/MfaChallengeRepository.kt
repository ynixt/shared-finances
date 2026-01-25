package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.mfa.MfaChallengeEntity
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface MfaChallengeRepository : EntityRepository<MfaChallengeEntity> {
    fun consumeChallengeReturningUserId(
        id: UUID,
        now: OffsetDateTime,
    ): Mono<UUID>

    fun deleteAllExpired(): Mono<Long>
}
