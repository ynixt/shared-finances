package com.ynixt.sharedfinances.resources.repositories.redis

import java.util.UUID

internal object AuthRedisKeys {
    fun session(id: UUID): String = "sf:auth:session:$id"

    fun sessionRefreshIndex(sessionId: UUID): String = "sf:auth:session:$sessionId:rth"

    fun refreshToken(tokenHashHex: String): String = "sf:auth:rt:$tokenHashHex"

    /** Set of session id strings for a user (for invalidation on account deletion). */
    fun userSessions(userId: UUID): String = "sf:auth:user:$userId:sessions"

    fun emailVerifyToken(tokenHashHex: String): String = "sf:auth:email-verify:token:$tokenHashHex"

    fun emailVerifyActive(userId: UUID): String = "sf:auth:email-verify:active:$userId"

    fun emailVerifyResend(userId: UUID): String = "sf:auth:email-verify:resend:$userId"

    fun emailVerifyResendForEmail(normalizedEmail: String): String = "sf:auth:email-verify:resend:e:$normalizedEmail"

    fun passwordResetToken(tokenHashHex: String): String = "sf:auth:password-reset:token:$tokenHashHex"

    fun passwordResetActive(userId: UUID): String = "sf:auth:password-reset:active:$userId"

    fun passwordResetResend(userId: UUID): String = "sf:auth:password-reset:resend:$userId"

    /** Cooldown for forgot-password request/resend, keyed by normalized email (no IP). */
    fun passwordResetResendForEmail(normalizedEmail: String): String = "sf:auth:password-reset:resend:e:$normalizedEmail"

    fun mailQuota(
        providerId: String,
        dayUtc: String,
    ): String = "sf:mail:quota:$providerId:$dayUtc"
}

internal fun ByteArray.toHexLower(): String = joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
