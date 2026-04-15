package com.ynixt.sharedfinances.resources.repositories.redis

import com.ynixt.sharedfinances.domain.entities.mfa.MfaChallengeEntity
import com.ynixt.sharedfinances.domain.repositories.MfaChallengeRepository
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class MfaChallengeRedisRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val clock: Clock,
) : MfaChallengeRepository {
    private val consumeChallengeScript =
        DefaultRedisScript<String>().apply {
            setScriptText(
                """
                local userId = redis.call('HGET', KEYS[1], 'userId')
                if userId == false then return nil end
                local expMs = redis.call('HGET', KEYS[1], 'expiresAtMillis')
                if expMs == false then
                  redis.call('DEL', KEYS[1])
                  return nil
                end
                if tonumber(expMs) < tonumber(ARGV[1]) then
                  redis.call('DEL', KEYS[1])
                  return nil
                end
                redis.call('DEL', KEYS[1])
                return userId
                """.trimIndent(),
            )
            resultType = String::class.java
        }

    override fun findById(id: UUID): Mono<MfaChallengeEntity> {
        val key = AuthRedisKeys.mfaChallenge(id)
        return redisTemplate
            .opsForHash<String, String>()
            .entries(key)
            .collectMap({ it.key }, { it.value })
            .filter { it.isNotEmpty() }
            .map { it.toMfaChallengeEntity(id) }
    }

    override fun deleteById(id: UUID): Mono<Long> = redisTemplate.delete(AuthRedisKeys.mfaChallenge(id))

    override fun existsById(id: UUID): Mono<Boolean> = redisTemplate.hasKey(AuthRedisKeys.mfaChallenge(id)).defaultIfEmpty(false)

    override fun <S : MfaChallengeEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        val key = AuthRedisKeys.mfaChallenge(id)
        val ttl = ttlUntilExpiresAt(entity.expiresAt, clock)
        return redisTemplate
            .opsForHash<String, String>()
            .putAll(key, entity.toRedisHash())
            .then(redisTemplate.expire(key, ttl))
            .thenReturn(entity)
    }

    override fun <S : MfaChallengeEntity> saveAll(entities: Iterable<S>): Flux<S> = Flux.fromIterable(entities).concatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<MfaChallengeEntity> = Flux.fromIterable(id).flatMap { findById(it) }

    override fun consumeChallengeReturningUserId(
        id: UUID,
        now: OffsetDateTime,
    ): Mono<UUID> {
        val key = AuthRedisKeys.mfaChallenge(id)
        val nowMs = now.toInstant().toEpochMilli().toString()
        return redisTemplate
            .execute(consumeChallengeScript, listOf(key), nowMs)
            .next()
            .map { UUID.fromString(it) }
    }
}
