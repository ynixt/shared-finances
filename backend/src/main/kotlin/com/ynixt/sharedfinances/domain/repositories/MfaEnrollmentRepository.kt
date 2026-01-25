package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.mfa.MfaEnrollmentEntity
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface MfaEnrollmentRepository : EntityRepository<MfaEnrollmentEntity> {
    fun deleteAllByUserId(userId: UUID): Mono<Long>

    fun deleteAllExpired(): Mono<Long>

    fun consumeValidEnrollmentReturningSecret(
        id: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ): Mono<String>
}
