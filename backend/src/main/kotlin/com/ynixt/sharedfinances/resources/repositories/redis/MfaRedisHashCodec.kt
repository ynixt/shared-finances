package com.ynixt.sharedfinances.resources.repositories.redis

import com.ynixt.sharedfinances.domain.entities.mfa.MfaChallengeEntity
import com.ynixt.sharedfinances.domain.entities.mfa.MfaEnrollmentEntity
import java.net.InetAddress
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

internal object MfaRedisHashFields {
    const val USER_ID = "userId"
    const val USER_AGENT = "userAgent"
    const val IP = "ip"
    const val CREATED_AT = "createdAt"
    const val EXPIRES_AT = "expiresAt"
    const val EXPIRES_AT_MILLIS = "expiresAtMillis"
    const val SECRET_ENC = "secretEnc"
}

internal fun ttlUntilExpiresAt(
    expiresAt: OffsetDateTime,
    clock: Clock,
): java.time.Duration {
    val now = OffsetDateTime.now(clock)
    val d = java.time.Duration.between(now.toInstant(), expiresAt.toInstant())
    return if (d.isNegative || d < java.time.Duration.ofSeconds(1)) {
        java.time.Duration.ofSeconds(1)
    } else {
        d
    }
}

internal fun MfaChallengeEntity.toRedisHash(): Map<String, String> {
    val expiresAtMillis = expiresAt.toInstant().toEpochMilli()
    return mapOf(
        MfaRedisHashFields.USER_ID to userId.toString(),
        MfaRedisHashFields.USER_AGENT to (userAgent ?: ""),
        MfaRedisHashFields.IP to (ip?.hostAddress ?: ""),
        MfaRedisHashFields.CREATED_AT to createdAt.toString(),
        MfaRedisHashFields.EXPIRES_AT to expiresAt.toString(),
        MfaRedisHashFields.EXPIRES_AT_MILLIS to expiresAtMillis.toString(),
    )
}

internal fun Map<String, String>.toMfaChallengeEntity(id: UUID): MfaChallengeEntity {
    val userId = UUID.fromString(this[MfaRedisHashFields.USER_ID]!!)
    val userAgent = this[MfaRedisHashFields.USER_AGENT]?.takeIf { it.isNotEmpty() }
    val ipStr = this[MfaRedisHashFields.IP]?.takeIf { it.isNotEmpty() }
    val ip = ipStr?.let { InetAddress.getByName(it) }
    val createdAt = OffsetDateTime.parse(this[MfaRedisHashFields.CREATED_AT]!!)
    val expiresAt = OffsetDateTime.parse(this[MfaRedisHashFields.EXPIRES_AT]!!)
    return MfaChallengeEntity(
        userId = userId,
        userAgent = userAgent,
        ip = ip,
        createdAt = createdAt,
        expiresAt = expiresAt,
    ).also { it.id = id }
}

internal fun MfaEnrollmentEntity.toRedisHash(): Map<String, String> {
    val expiresAtMillis = expiresAt.toInstant().toEpochMilli()
    return mapOf(
        MfaRedisHashFields.USER_ID to userId.toString(),
        MfaRedisHashFields.SECRET_ENC to secretEnc,
        MfaRedisHashFields.CREATED_AT to createdAt.toString(),
        MfaRedisHashFields.EXPIRES_AT to expiresAt.toString(),
        MfaRedisHashFields.EXPIRES_AT_MILLIS to expiresAtMillis.toString(),
    )
}

internal fun Map<String, String>.toMfaEnrollmentEntity(id: UUID): MfaEnrollmentEntity {
    val userId = UUID.fromString(this[MfaRedisHashFields.USER_ID]!!)
    val secretEnc = this[MfaRedisHashFields.SECRET_ENC]!!
    val createdAt = OffsetDateTime.parse(this[MfaRedisHashFields.CREATED_AT]!!)
    val expiresAt = OffsetDateTime.parse(this[MfaRedisHashFields.EXPIRES_AT]!!)
    return MfaEnrollmentEntity(
        userId = userId,
        secretEnc = secretEnc,
        createdAt = createdAt,
        expiresAt = expiresAt,
    ).also { it.id = id }
}
