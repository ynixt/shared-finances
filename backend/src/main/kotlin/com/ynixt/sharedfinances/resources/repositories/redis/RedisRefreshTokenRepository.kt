package com.ynixt.sharedfinances.resources.repositories.redis

import com.ynixt.sharedfinances.domain.entities.RefreshTokenEntity
import com.ynixt.sharedfinances.domain.repositories.RefreshTokenRepository
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Repository
class RedisRefreshTokenRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : RefreshTokenRepository {
    override fun deleteByTokenHash(tokenHash: ByteArray): Mono<Boolean> {
        val hex = tokenHash.toHexLower()
        val key = AuthRedisKeys.refreshToken(hex)
        return redisTemplate
            .opsForValue()
            .get(key)
            .flatMap { json ->
                val p = objectMapper.readValue(json, AuthRefreshRedisPayload::class.java)
                redisTemplate
                    .opsForSet()
                    .remove(AuthRedisKeys.sessionRefreshIndex(p.sessionId), hex)
                    .then(redisTemplate.delete(key))
                    .map { deleted -> deleted > 0 }
            }.switchIfEmpty(Mono.just(false))
    }

    override fun findByTokenHashAndExpiresAtAfter(
        tokenHash: ByteArray,
        expiresAt: Instant,
    ): Mono<RefreshTokenEntity> {
        val key = AuthRedisKeys.refreshToken(tokenHash.toHexLower())
        return redisTemplate
            .opsForValue()
            .get(key)
            .flatMap { json ->
                val p = objectMapper.readValue(json, AuthRefreshRedisPayload::class.java)
                val tokenExpires = Instant.ofEpochMilli(p.expiresAtEpochMilli)
                if (!tokenExpires.isAfter(expiresAt)) {
                    Mono.empty()
                } else {
                    Mono.just(
                        RefreshTokenEntity(
                            sessionId = p.sessionId,
                            createdAt = Instant.ofEpochMilli(p.createdAtEpochMilli),
                            tokenHash = tokenHash,
                            expiresAt = tokenExpires,
                        ).apply { id = p.id },
                    )
                }
            }
    }

    override fun findById(id: UUID): Mono<RefreshTokenEntity> = Mono.empty()

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(false)

    override fun <S : RefreshTokenEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        val hashHex = entity.tokenHash.toHexLower()
        val key = AuthRedisKeys.refreshToken(hashHex)
        val now = Instant.now()
        val ttlSeconds = Duration.between(now, entity.expiresAt).seconds.coerceAtLeast(1L)
        val payload =
            AuthRefreshRedisPayload(
                id = id,
                sessionId = entity.sessionId,
                createdAtEpochMilli = entity.createdAt.toEpochMilli(),
                expiresAtEpochMilli = entity.expiresAt.toEpochMilli(),
            )
        val json = objectMapper.writeValueAsString(payload)
        return redisTemplate
            .opsForValue()
            .set(key, json, Duration.ofSeconds(ttlSeconds))
            .then(
                redisTemplate
                    .opsForSet()
                    .add(AuthRedisKeys.sessionRefreshIndex(entity.sessionId), hashHex),
            ).then(
                Mono.fromCallable {
                    entity.id = id
                    @Suppress("UNCHECKED_CAST")
                    entity as S
                },
            )
    }

    override fun <S : RefreshTokenEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<RefreshTokenEntity> = Flux.empty()
}
