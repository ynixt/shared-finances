package com.ynixt.sharedfinances.resources.repositories.redis

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Repository
class MailProviderQuotaRedisRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val clock: Clock,
) {
    private val reserveScript =
        DefaultRedisScript<Long>().apply {
            setScriptText(
                """
                local c = redis.call('INCR', KEYS[1])
                if c == 1 then
                  redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
                end
                if c > tonumber(ARGV[2]) then
                  redis.call('DECR', KEYS[1])
                  return 0
                end
                return 1
                """.trimIndent(),
            )
            resultType = Long::class.java
        }

    private val rollbackScript =
        DefaultRedisScript<Long>().apply {
            setScriptText(
                """
                local c = redis.call('GET', KEYS[1])
                if c == false then return 0 end
                local n = tonumber(c)
                if n == nil or n <= 0 then return 0 end
                return redis.call('DECR', KEYS[1])
                """.trimIndent(),
            )
            resultType = Long::class.java
        }

    /**
     * @param dailyQuota `null` or `<= 0` — no daily cap (always succeeds, no Redis counter).
     */
    fun tryReserveOne(
        providerId: String,
        dailyQuota: Int?,
    ): Mono<Boolean> {
        if (dailyQuota == null || dailyQuota <= 0) {
            return Mono.just(true)
        }
        val key = quotaKey(providerId)
        val ttl = secondsUntilEndOfUtcDay()
        return redisTemplate
            .execute(
                reserveScript,
                listOf(key),
                ttl.toString(),
                dailyQuota.toString(),
            ).next()
            .map { it == 1L }
            .defaultIfEmpty(false)
    }

    fun rollbackOne(providerId: String): Mono<Long> =
        redisTemplate
            .execute(
                rollbackScript,
                listOf(quotaKey(providerId)),
            ).next()
            .defaultIfEmpty(0L)

    private fun quotaKey(providerId: String): String {
        val day = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(clock.instant())
        return AuthRedisKeys.mailQuota(providerId, day)
    }

    private fun secondsUntilEndOfUtcDay(): Long {
        val z = clock.instant().atZone(ZoneOffset.UTC)
        val end =
            z
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
        return Duration.between(clock.instant(), end).seconds.coerceAtLeast(1L)
    }
}
