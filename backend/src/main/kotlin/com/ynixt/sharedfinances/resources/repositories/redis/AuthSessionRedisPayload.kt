package com.ynixt.sharedfinances.resources.repositories.redis

import java.util.UUID

internal data class AuthSessionRedisPayload(
    val userId: UUID,
    val userAgent: String?,
    val ip: String?,
)

internal data class AuthRefreshRedisPayload(
    val id: UUID,
    val sessionId: UUID,
    val createdAtEpochMilli: Long,
    val expiresAtEpochMilli: Long,
)
