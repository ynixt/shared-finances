package com.ynixt.sharedfinances.resources.repositories.redis

import com.ynixt.sharedfinances.domain.repositories.AvatarPresignedUrlCacheRepository
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

@Repository
class AvatarPresignedUrlRedisRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
) : AvatarPresignedUrlCacheRepository {
    override fun findByOwnerId(ownerId: UUID): Mono<String> =
        redisTemplate.opsForValue().get(
            AvatarRedisKeys.avatarPresignedUrl(ownerId),
        )

    override fun save(
        ownerId: UUID,
        presignedUrl: String,
        ttl: Duration,
    ): Mono<Boolean> =
        redisTemplate.opsForValue().set(
            AvatarRedisKeys.avatarPresignedUrl(ownerId),
            presignedUrl,
            ttl,
        )

    override fun delete(ownerId: UUID): Mono<Boolean> =
        redisTemplate.opsForValue().delete(
            AvatarRedisKeys.avatarPresignedUrl(ownerId),
        )
}
