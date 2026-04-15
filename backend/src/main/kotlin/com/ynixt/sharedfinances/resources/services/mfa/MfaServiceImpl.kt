package com.ynixt.sharedfinances.resources.services.mfa

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.mfa.MfaChallengeEntity
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidCredentialsException
import com.ynixt.sharedfinances.domain.repositories.MfaChallengeRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.mfa.MfaSecretCryptoService
import com.ynixt.sharedfinances.domain.services.mfa.MfaService
import com.ynixt.sharedfinances.domain.services.mfa.TotpService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MfaServiceImpl(
    private val mfaSecretCryptoService: MfaSecretCryptoService,
    private val totpService: TotpService,
    private val mfaChallengeRepository: MfaChallengeRepository,
    private val userRepository: UserRepository,
) : MfaService {
    override fun decryptAndVerify(
        secret: String,
        code: String,
    ): Boolean {
        val rawSecret = mfaSecretCryptoService.decryptTotpSecret(secret)

        return totpService.verifyRaw(rawSecret, code)
    }

    @Transactional
    override suspend fun verifyChallenge(
        challengeId: UUID,
        code: String,
        ip: InetAddress?,
    ): UserEntity {
        val now = OffsetDateTime.now()

        val challenge =
            mfaChallengeRepository.findById(challengeId).awaitSingleOrNull()
                ?: throw InvalidCredentialsException(
                    email = null,
                    ip = ip.toString(),
                )

        if (!challenge.expiresAt.isAfter(now)) {
            throw InvalidCredentialsException(
                email = null,
                ip = ip.toString(),
            )
        }

        val user =
            userRepository.findById(challenge.userId).awaitSingleOrNull()
                ?: throw InvalidCredentialsException(
                    email = null,
                    ip = ip.toString(),
                )

        if (!user.mfaEnabled || user.totpSecret == null) {
            throw IllegalStateException("MFA not enabled")
        }

        if (!decryptAndVerify(secret = user.totpSecret!!, code = code)) {
            throw InvalidCredentialsException(
                email = user.email,
                ip = ip.toString(),
            )
        }

        mfaChallengeRepository
            .consumeChallengeReturningUserId(challengeId, now)
            .awaitSingleOrNull()
            ?: throw InvalidCredentialsException(
                email = user.email,
                ip = ip.toString(),
            )

        return user
    }

    override suspend fun generateNewChallenge(
        userId: UUID,
        userAgent: String?,
        ip: InetAddress?,
    ): UUID =
        mfaChallengeRepository
            .save(
                MfaChallengeEntity(
                    userId = userId,
                    userAgent = userAgent,
                    ip = ip,
                ),
            ).map {
                it.id!!
            }.awaitSingle()
}
