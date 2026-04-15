package com.ynixt.sharedfinances.domain.entities.mfa

import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import java.net.InetAddress
import java.time.OffsetDateTime
import java.util.UUID

class MfaChallengeEntity(
    val userId: UUID,
    val userAgent: String?,
    val ip: InetAddress?,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val expiresAt: OffsetDateTime = OffsetDateTime.now().plusMinutes(5),
) : SimpleEntity()
