package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.mfa.MfaRecoveryCodeEntity
import com.ynixt.sharedfinances.domain.repositories.MfaRecoveryCodeRepository
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono
import java.util.UUID

interface MfaRecoveryCodeSpringDataRepository :
    MfaRecoveryCodeRepository,
    Repository<MfaRecoveryCodeEntity, String> {
    @Query(
        """
        delete 
        from mfa_recovery_codes 
        where user_id = :userId 
        and used_at is null
    """,
    )
    override fun deleteAllUnusedByUserId(userId: UUID): Mono<Long>
}
