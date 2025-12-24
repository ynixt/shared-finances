package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.RefreshTokenEntity
import reactor.core.publisher.Mono
import java.time.Instant

interface RefreshTokenRepository : EntityRepository<RefreshTokenEntity> {
    fun deleteByTokenHash(tokenHash: ByteArray): Mono<Boolean>

    fun findByTokenHashAndExpiresAtAfter(
        tokenHash: ByteArray,
        expiresAt: Instant,
    ): Mono<RefreshTokenEntity>
}
