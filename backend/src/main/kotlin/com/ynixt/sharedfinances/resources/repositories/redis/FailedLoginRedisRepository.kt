package com.ynixt.sharedfinances.resources.repositories.redis

import com.ynixt.sharedfinances.domain.documents.FailedLoginDocument
import com.ynixt.sharedfinances.domain.repositories.FailedLoginRepository
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
class FailedLoginRedisRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
) : FailedLoginRepository {
    private val incrWithTtlScript =
        DefaultRedisScript<Long>().apply {
            setScriptText(
                """
                local v = redis.call('INCR', KEYS[1])
                if v == 1 then
                  redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return v
                """.trimIndent(),
            )
            resultType = Long::class.java
        }

    override fun incrementFails(
        ip: String,
        email: String,
        ttlSeconds: Long,
    ): Mono<Void> =
        redisTemplate
            .execute(
                incrWithTtlScript,
                listOf(FailedLoginDocument.buildId(ip = ip, email = email)),
                ttlSeconds.toString(),
            ).single()
            .then()

    override fun deleteByIpAndEmail(
        ip: String,
        email: String,
    ): Mono<Boolean> =
        redisTemplate.delete(FailedLoginDocument.buildId(ip = ip, email = email)).map {
            it > 0
        }

    override fun getFails(
        ip: String,
        email: String,
    ): Mono<Int> {
        val key = FailedLoginDocument.buildId(ip = ip, email = email)

        return redisTemplate
            .opsForValue()
            .get(key)
            .map { it.toString().toInt() }
            .defaultIfEmpty(0)
    }
}
