package com.ynixt.sharedfinances.domain.services.mfa.impl

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.mfa.MfaChallengeEntity
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidCredentialsException
import com.ynixt.sharedfinances.domain.repositories.MfaChallengeRepository
import com.ynixt.sharedfinances.domain.repositories.MfaEnrollmentRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.mfa.MfaSecretCryptoService
import com.ynixt.sharedfinances.domain.services.mfa.MfaService
import com.ynixt.sharedfinances.domain.services.mfa.TotpService
import io.lettuce.core.KillArgs.Builder.user
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
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
    override fun verifyChallenge(
        challengeId: UUID,
        code: String,
        ip: InetAddress?,
    ): Mono<UserEntity> {
        val now = OffsetDateTime.now()

        return mfaChallengeRepository
            .consumeChallengeReturningUserId(challengeId, now)
            .flatMap { userId ->
                userRepository.findById(userId).flatMap { user ->
                    if (!user.mfaEnabled || user.totpSecret == null) return@flatMap Mono.error(IllegalStateException("MFA not enabled"))

                    if (decryptAndVerify(
                            secret = user.totpSecret!!,
                            code = code,
                        )
                    ) {
                        Mono.just(user)
                    } else {
                        Mono.error(
                            InvalidCredentialsException(
                                email = user.email,
                                ip = ip.toString(),
                            ),
                        )
                    }
                }
            }.switchIfEmpty(
                Mono.error(
                    InvalidCredentialsException(
                        email = null,
                        ip = ip.toString(),
                    ),
                ),
            )
    }

    override fun generateNewChallenge(
        userId: UUID,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<UUID> =
        mfaChallengeRepository
            .save(
                MfaChallengeEntity(
                    userId = userId,
                    userAgent = userAgent,
                    ip = ip,
                ),
            ).map {
                it.id!!
            }

    override fun expireOld(): Mono<Void> =
        mfaChallengeRepository
            .deleteAllExpired()
            .then(
                mfaEnrollmentRepository.deleteAllExpired(),
            ).then()
}
