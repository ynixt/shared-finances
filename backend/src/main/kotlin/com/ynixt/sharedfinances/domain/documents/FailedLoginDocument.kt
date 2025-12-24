package com.ynixt.sharedfinances.domain.documents

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash

@RedisHash("failed_logins")
data class FailedLoginDocument(
    val ip: String,
    val email: String,
) {
    @Id
    val id: String = buildId(ip, email)

    companion object {
        fun buildId(
            ip: String,
            email: String,
        ): String = "$ip|$email"
    }
}
