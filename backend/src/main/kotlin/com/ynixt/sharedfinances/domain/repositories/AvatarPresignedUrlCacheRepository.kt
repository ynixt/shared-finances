package com.ynixt.sharedfinances.domain.repositories

import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

interface AvatarPresignedUrlCacheRepository {
    fun findByOwnerId(ownerId: UUID): Mono<String>

    fun save(
        ownerId: UUID,
        presignedUrl: String,
        ttl: Duration,
    ): Mono<Boolean>

    fun delete(ownerId: UUID): Mono<Boolean>
}
