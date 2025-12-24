package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("refresh_tokens")
class RefreshTokenEntity(
    val sessionId: UUID,
    val createdAt: Instant,
    val tokenHash: ByteArray,
    val expiresAt: Instant,
) : SimpleEntity()
