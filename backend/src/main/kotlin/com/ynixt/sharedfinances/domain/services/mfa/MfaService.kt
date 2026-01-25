package com.ynixt.sharedfinances.domain.services.mfa

import com.ynixt.sharedfinances.domain.entities.UserEntity
import reactor.core.publisher.Mono
import java.net.InetAddress
import java.util.UUID

interface MfaService {
    fun decryptAndVerify(
        secret: String,
        code: String,
    ): Boolean

    fun generateNewChallenge(
        userId: UUID,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<UUID>

    fun verifyChallenge(
        challengeId: UUID,
        code: String,
        ip: InetAddress?,
    ): Mono<UserEntity>

    fun expireOld(): Mono<Void>
}
