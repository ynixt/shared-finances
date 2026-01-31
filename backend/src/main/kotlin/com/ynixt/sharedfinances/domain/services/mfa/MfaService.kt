package com.ynixt.sharedfinances.domain.services.mfa

import com.ynixt.sharedfinances.domain.entities.UserEntity
import java.net.InetAddress
import java.util.UUID

interface MfaService {
    fun decryptAndVerify(
        secret: String,
        code: String,
    ): Boolean

    suspend fun generateNewChallenge(
        userId: UUID,
        userAgent: String?,
        ip: InetAddress?,
    ): UUID

    suspend fun verifyChallenge(
        challengeId: UUID,
        code: String,
        ip: InetAddress?,
    ): UserEntity

    suspend fun expireOld()
}
