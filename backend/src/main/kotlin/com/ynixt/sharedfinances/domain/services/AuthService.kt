package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.application.web.dto.auth.LoginResultDto
import reactor.core.publisher.Mono
import java.net.InetAddress
import java.util.UUID

const val SESSION_CLAIM_NAME = "session"

interface AuthService {
    fun login(
        email: String,
        passwordHash: String,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<LoginResultDto>

    fun logout(session: UUID): Mono<Void>

    fun refreshToken(refreshToken: String): Mono<String>
}
