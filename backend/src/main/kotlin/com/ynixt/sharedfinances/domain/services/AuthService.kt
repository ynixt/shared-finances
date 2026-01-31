package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.models.LoginResult
import java.net.InetAddress
import java.util.UUID

const val SESSION_CLAIM_NAME = "session"

interface AuthService {
    suspend fun login(
        email: String,
        rawPassword: String,
        userAgent: String?,
        ip: InetAddress?,
    ): LoginResult

    suspend fun mfa(
        challengeId: UUID,
        code: String,
        userAgent: String?,
        ip: InetAddress?,
    ): LoginResult

    suspend fun logout(session: UUID)

    suspend fun refreshToken(refreshToken: String): String

    suspend fun checkPassword(
        userId: UUID,
        rawPassword: String,
    ): UserEntity
}
