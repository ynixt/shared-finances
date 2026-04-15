package com.ynixt.sharedfinances.domain.entities

import java.time.Instant
import java.util.UUID

class RefreshTokenEntity(
    val sessionId: UUID,
    val createdAt: Instant,
    val tokenHash: ByteArray,
    val expiresAt: Instant,
) : SimpleEntity()
