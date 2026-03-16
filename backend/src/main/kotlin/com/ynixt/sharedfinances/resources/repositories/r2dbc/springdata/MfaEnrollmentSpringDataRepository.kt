package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.mfa.MfaEnrollmentEntity
import com.ynixt.sharedfinances.domain.repositories.MfaEnrollmentRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface MfaEnrollmentSpringDataRepository :
    MfaEnrollmentRepository,
    R2dbcRepository<MfaEnrollmentEntity, String> {
    @Query(
        """
        DELETE FROM mfa_enrollments
        WHERE id = :id
          AND user_id = :userId
          AND expires_at > :now
        RETURNING secret_enc
    """,
    )
    override fun consumeValidEnrollmentReturningSecret(
        id: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ): Mono<String>

    @Modifying
    @Query(
        """
        delete from mfa_enrollments where expires_at < CURRENT_TIMESTAMP
    """,
    )
    override fun deleteAllExpired(): Mono<Long>
}
