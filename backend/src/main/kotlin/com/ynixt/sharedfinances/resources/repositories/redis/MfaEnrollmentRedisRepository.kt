package com.ynixt.sharedfinances.resources.repositories.redis

import com.ynixt.sharedfinances.domain.entities.mfa.MfaEnrollmentEntity
import com.ynixt.sharedfinances.domain.repositories.MfaEnrollmentRepository
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class MfaEnrollmentRedisRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val clock: Clock,
) : MfaEnrollmentRepository {
    private val consumeEnrollmentScript =
        DefaultRedisScript<String>().apply {
            setScriptText(
                """
                local uid = redis.call('HGET', KEYS[1], 'userId')
                if uid == false then return nil end
                local expMs = redis.call('HGET', KEYS[1], 'expiresAtMillis')
                if expMs == false then
                  redis.call('DEL', KEYS[1])
                  redis.call('DEL', KEYS[2])
                  return nil
                end
                if tonumber(expMs) < tonumber(ARGV[1]) then
                  redis.call('DEL', KEYS[1])
                  redis.call('DEL', KEYS[2])
                  return nil
                end
                if uid ~= ARGV[2] then
                  return '__DENY__'
                end
                local sec = redis.call('HGET', KEYS[1], 'secretEnc')
                redis.call('DEL', KEYS[1])
                redis.call('DEL', KEYS[2])
                return sec
                """.trimIndent(),
            )
            resultType = String::class.java
        }

    override fun findById(id: UUID): Mono<MfaEnrollmentEntity> {
        val key = AuthRedisKeys.mfaEnrollment(id)
        return redisTemplate
            .opsForHash<String, String>()
            .entries(key)
            .collectMap({ it.key }, { it.value })
            .filter { it.isNotEmpty() }
            .map { it.toMfaEnrollmentEntity(id) }
    }

    override fun deleteById(id: UUID): Mono<Long> {
        val enrollmentKey = AuthRedisKeys.mfaEnrollment(id)
        return redisTemplate
            .opsForHash<String, String>()
            .get(enrollmentKey, MfaRedisHashFields.USER_ID)
            .flatMap { uidStr ->
                val userId = UUID.fromString(uidStr)
                val pointerKey = AuthRedisKeys.mfaEnrollmentPending(userId)
                redisTemplate
                    .opsForValue()
                    .get(pointerKey)
                    .flatMap { storedId ->
                        if (storedId == id.toString()) {
                            redisTemplate.delete(pointerKey, enrollmentKey)
                        } else {
                            redisTemplate.delete(enrollmentKey)
                        }
                    }.switchIfEmpty(redisTemplate.delete(enrollmentKey))
            }.switchIfEmpty(redisTemplate.delete(enrollmentKey))
    }

    override fun existsById(id: UUID): Mono<Boolean> = redisTemplate.hasKey(AuthRedisKeys.mfaEnrollment(id)).defaultIfEmpty(false)

    override fun <S : MfaEnrollmentEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        val enrollmentKey = AuthRedisKeys.mfaEnrollment(id)
        val pointerKey = AuthRedisKeys.mfaEnrollmentPending(entity.userId)
        val ttl = ttlUntilExpiresAt(entity.expiresAt, clock)
        val hash = entity.toRedisHash()
        return redisTemplate
            .opsForHash<String, String>()
            .putAll(enrollmentKey, hash)
            .then(redisTemplate.expire(enrollmentKey, ttl))
            .then(
                redisTemplate
                    .opsForValue()
                    .set(pointerKey, id.toString(), ttl),
            ).thenReturn(entity)
    }

    override fun <S : MfaEnrollmentEntity> saveAll(entities: Iterable<S>): Flux<S> = Flux.fromIterable(entities).concatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<MfaEnrollmentEntity> = Flux.fromIterable(id).flatMap { findById(it) }

    override fun deleteAllByUserId(userId: UUID): Mono<Long> {
        val pointerKey = AuthRedisKeys.mfaEnrollmentPending(userId)
        return redisTemplate
            .opsForValue()
            .get(pointerKey)
            .flatMap { enrollmentIdStr ->
                val enrollmentKey = AuthRedisKeys.mfaEnrollment(UUID.fromString(enrollmentIdStr))
                redisTemplate.delete(enrollmentKey, pointerKey)
            }.switchIfEmpty(redisTemplate.delete(pointerKey))
    }

    override fun consumeValidEnrollmentReturningSecret(
        id: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ): Mono<String> {
        val key = AuthRedisKeys.mfaEnrollment(id)
        val pointerKey = AuthRedisKeys.mfaEnrollmentPending(userId)
        val nowMs = now.toInstant().toEpochMilli().toString()
        return redisTemplate
            .execute(consumeEnrollmentScript, listOf(key, pointerKey), nowMs, userId.toString())
            .next()
            .filter { it != "__DENY__" }
    }
}
