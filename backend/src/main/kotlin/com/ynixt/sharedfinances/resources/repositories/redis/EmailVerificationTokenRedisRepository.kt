package com.ynixt.sharedfinances.resources.repositories.redis

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID

data class EmailVerificationTokenPayload(
    val userId: UUID,
    val email: String,
)

@Repository
class EmailVerificationTokenRedisRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val getDelScript =
        DefaultRedisScript<String>().apply {
            setScriptText(
                """
                local v = redis.call('GET', KEYS[1])
                if v == false then return nil end
                redis.call('DEL', KEYS[1])
                return v
                """.trimIndent(),
            )
            resultType = String::class.java
        }

    fun issueToken(
        userId: UUID,
        email: String,
        rawToken: String,
        ttl: Duration,
    ): Mono<Void> {
        val hashHex = sha256Hex(rawToken)
        val payload = EmailVerificationTokenPayload(userId = userId, email = email.lowercase())
        val json = objectMapper.writeValueAsString(payload)
        val tokenKey = AuthRedisKeys.emailVerifyToken(hashHex)
        val activeKey = AuthRedisKeys.emailVerifyActive(userId)

        return redisTemplate
            .opsForValue()
            .get(activeKey)
            .filter { it.isNotBlank() }
            .flatMap { oldHash -> redisTemplate.delete(AuthRedisKeys.emailVerifyToken(oldHash)) }
            .then(
                redisTemplate
                    .opsForValue()
                    .set(tokenKey, json, ttl),
            ).then(
                redisTemplate
                    .opsForValue()
                    .set(activeKey, hashHex, ttl),
            ).then()
    }

    fun consumeRawToken(rawToken: String): Mono<EmailVerificationTokenPayload> {
        val hashHex = sha256Hex(rawToken)
        val tokenKey = AuthRedisKeys.emailVerifyToken(hashHex)
        return redisTemplate
            .execute(
                getDelScript,
                listOf(tokenKey),
            ).next()
            .map { json ->
                objectMapper.readValue(json, EmailVerificationTokenPayload::class.java)
            }
    }

    fun deleteActivePointer(userId: UUID): Mono<Long> = redisTemplate.delete(AuthRedisKeys.emailVerifyActive(userId))

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
    }
}
