package com.ynixt.sharedfinances.resources.repositories.redis

import com.ynixt.sharedfinances.domain.entities.SessionEntity
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper
import java.net.InetAddress
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import java.util.UUID

@Repository
class RedisSessionRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) : SessionRepository {
    override fun findById(id: UUID): Mono<SessionEntity> =
        redisTemplate
            .opsForValue()
            .get(AuthRedisKeys.session(id))
            .map { json ->
                val p = objectMapper.readValue(json, AuthSessionRedisPayload::class.java)
                SessionEntity(
                    userId = p.userId,
                    userAgent = p.userAgent,
                    ip = p.ip?.let { InetAddress.getByName(it) },
                ).also { it.id = id }
            }

    override fun deleteById(id: UUID): Mono<Long> {
        val sessionKey = AuthRedisKeys.session(id)
        val idx = AuthRedisKeys.sessionRefreshIndex(id)
        return redisTemplate
            .opsForValue()
            .get(sessionKey)
            .flatMap { json ->
                val userId =
                    runCatching {
                        objectMapper.readValue(json, AuthSessionRedisPayload::class.java).userId
                    }.getOrNull()
                cascadeDeleteSessionAndRefreshTokens(sessionKey, idx)
                    .flatMap { deleted ->
                        if (userId != null) {
                            redisTemplate
                                .opsForSet()
                                .remove(AuthRedisKeys.userSessions(userId), id.toString())
                                .thenReturn(deleted)
                        } else {
                            Mono.just(deleted)
                        }
                    }
            }.switchIfEmpty(cascadeDeleteSessionAndRefreshTokens(sessionKey, idx))
    }

    override fun deleteAllByUserId(userId: UUID): Mono<Long> {
        val userKey = AuthRedisKeys.userSessions(userId)
        return redisTemplate
            .opsForSet()
            .members(userKey)
            .collectList()
            .flatMap { sessionIdStrs ->
                val sessionDeletes: Mono<Long> =
                    if (sessionIdStrs.isEmpty()) {
                        Mono.just(0L)
                    } else {
                        Flux.fromIterable(sessionIdStrs)
                            .concatMap { sid -> deleteById(UUID.fromString(sid)) }
                            .reduce(0L) { acc, n -> acc + n }
                    }
                sessionDeletes.flatMap { deleted ->
                    redisTemplate
                        .delete(userKey)
                        .defaultIfEmpty(0L)
                        .map { keysRemoved -> deleted + keysRemoved }
                }
            }
    }

    override fun existsById(id: UUID): Mono<Boolean> = redisTemplate.hasKey(AuthRedisKeys.session(id))

    override fun <S : SessionEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        val now = clock.instant()
        val expiresAt = now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant()
        val ttlSeconds = Duration.between(now, expiresAt).seconds.coerceAtLeast(1L)
        val payload =
            AuthSessionRedisPayload(
                userId = entity.userId,
                userAgent = entity.userAgent,
                ip = entity.ip?.hostAddress,
            )
        val json = objectMapper.writeValueAsString(payload)
        val sessionKey = AuthRedisKeys.session(id)
        val userSet = AuthRedisKeys.userSessions(entity.userId)
        return redisTemplate
            .opsForValue()
            .set(sessionKey, json, Duration.ofSeconds(ttlSeconds))
            .then(redisTemplate.opsForSet().add(userSet, id.toString()))
            .then(
                Mono.fromCallable {
                    entity.id = id
                    @Suppress("UNCHECKED_CAST")
                    entity as S
                },
            )
    }

    override fun <S : SessionEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<SessionEntity> =
        Flux
            .fromIterable(id)
            .flatMap { findById(it) }

    private fun cascadeDeleteSessionAndRefreshTokens(
        sessionKey: String,
        idx: String,
    ): Mono<Long> =
        redisTemplate
            .opsForSet()
            .members(idx)
            .collectList()
            .flatMap { hashes ->
                val keysToDelete = hashes.map { AuthRedisKeys.refreshToken(it) } + idx + sessionKey
                if (keysToDelete.isEmpty()) {
                    Mono.just(0L)
                } else {
                    Flux
                        .fromIterable(keysToDelete.distinct())
                        .flatMap { redisTemplate.delete(it) }
                        .reduce(0L) { acc, c -> acc + c }
                }
            }
}
