package com.ynixt.sharedfinances.resources.repositories.redis

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration

@Repository
class AuthResendCooldownRedisRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
) {
    fun tryStartCooldown(
        key: String,
        cooldown: Duration,
    ): Mono<Boolean> =
        redisTemplate
            .opsForValue()
            .setIfAbsent(key, "1", cooldown)
            .defaultIfEmpty(false)

    fun remainingSeconds(key: String): Mono<Long> =
        redisTemplate
            .getExpire(key)
            .map { d ->
                when {
                    d.isNegative || d.isZero -> 0L
                    else -> d.seconds.coerceAtLeast(0L)
                }
            }.defaultIfEmpty(-1L)

    fun setCooldown(
        key: String,
        cooldown: Duration,
    ): Mono<Boolean> = redisTemplate.opsForValue().set(key, "1", cooldown)
}
