package com.ynixt.sharedfinances.resources.services.mfa

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.mfa.MfaChallengeEntity
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidCredentialsException
import com.ynixt.sharedfinances.domain.repositories.MfaChallengeRepository
import com.ynixt.sharedfinances.domain.repositories.MfaEnrollmentRepository
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
    private val mfaEnrollmentRepository: MfaEnrollmentRepository,
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

        return mfaChallengeRepository
            .consumeChallengeReturningUserId(challengeId, now)
            .awaitSingleOrNull()
            ?.let { userId ->
                userRepository.findById(userId).awaitSingleOrNull()?.let { user ->
                    if (!user.mfaEnabled || user.totpSecret == null) throw IllegalStateException("MFA not enabled")

                    if (decryptAndVerify(
                            secret = user.totpSecret!!,
                            code = code,
                        )
                    ) {
                        user
                    } else {
                        throw InvalidCredentialsException(
                            email = user.email,
                            ip = ip.toString(),
                        )
                    }
                }
            } ?: throw InvalidCredentialsException(
            email = null,
            ip = ip.toString(),
        )
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

    override suspend fun expireOld() {
        mfaChallengeRepository.deleteAllExpired().awaitSingle()
        mfaEnrollmentRepository.deleteAllExpired().awaitSingle()
    }
}
