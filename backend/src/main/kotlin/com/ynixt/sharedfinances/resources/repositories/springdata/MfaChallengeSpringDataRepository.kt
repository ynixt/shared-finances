package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.mfa.MfaChallengeEntity
import com.ynixt.sharedfinances.domain.repositories.MfaChallengeRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface MfaChallengeSpringDataRepository :
    MfaChallengeRepository,
    Repository<MfaChallengeEntity, String> {
    @Query(
        """
            delete from mfa_challenges
            where
                id = :id
                AND expires_at > :now
            RETURNING user_id
        """,
    )
    override fun consumeChallengeReturningUserId(
        id: UUID,
        now: OffsetDateTime,
    ): Mono<UUID>

    @Modifying
    @Query(
        """
        delete from mfa_challenges where expires_at < CURRENT_TIMESTAMP
    """,
    )
    override fun deleteAllExpired(): Mono<Long>
}
