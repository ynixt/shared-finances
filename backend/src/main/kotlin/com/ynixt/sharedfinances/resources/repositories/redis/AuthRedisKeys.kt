package com.ynixt.sharedfinances.resources.repositories.redis

import java.util.UUID

internal object AuthRedisKeys {
    fun session(id: UUID): String = "sf:auth:session:$id"

    fun sessionRefreshIndex(sessionId: UUID): String = "sf:auth:session:$sessionId:rth"

    fun refreshToken(tokenHashHex: String): String = "sf:auth:rt:$tokenHashHex"

    /** Set of session id strings for a user (for invalidation on account deletion). */
    fun userSessions(userId: UUID): String = "sf:auth:user:$userId:sessions"
}

internal fun ByteArray.toHexLower(): String = joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
