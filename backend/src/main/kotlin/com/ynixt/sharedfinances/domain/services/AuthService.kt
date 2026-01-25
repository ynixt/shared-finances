package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.models.LoginResult
import reactor.core.publisher.Mono
import java.net.InetAddress
import java.util.UUID

const val SESSION_CLAIM_NAME = "session"

interface AuthService {
    fun login(
        email: String,
        rawPassword: String,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<LoginResult>

    fun mfa(
        challengeId: UUID,
        code: String,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<LoginResult>

    fun logout(session: UUID): Mono<Void>

    fun refreshToken(refreshToken: String): Mono<String>

    fun checkPassword(
        userId: UUID,
        rawPassword: String,
    ): Mono<UserEntity>
}
