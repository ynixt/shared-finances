package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.mfa.MfaRecoveryCodeEntity
import reactor.core.publisher.Mono
import java.util.UUID

interface MfaRecoveryCodeRepository : EntityRepository<MfaRecoveryCodeEntity> {
    fun deleteAllUnusedByUserId(userId: UUID): Mono<Long>

    fun deleteAllByUserId(userId: UUID): Mono<Long>
}
