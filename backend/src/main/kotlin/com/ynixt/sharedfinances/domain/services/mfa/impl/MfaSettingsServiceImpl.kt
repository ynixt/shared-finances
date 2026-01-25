package com.ynixt.sharedfinances.domain.services.mfa.impl

import com.ynixt.sharedfinances.application.web.dto.auth.mfa.ConfirmMfaResponseDto
import com.ynixt.sharedfinances.application.web.dto.auth.mfa.EnableMfaResponseDto
import com.ynixt.sharedfinances.domain.entities.mfa.MfaEnrollmentEntity
import com.ynixt.sharedfinances.domain.entities.mfa.MfaRecoveryCodeEntity
import com.ynixt.sharedfinances.domain.exceptions.http.mfa.MfaAlreadyEnabledException
import com.ynixt.sharedfinances.domain.exceptions.http.mfa.MfaEnrollmentNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.mfa.WrongMfaCodeException
import com.ynixt.sharedfinances.domain.repositories.MfaEnrollmentRepository
import com.ynixt.sharedfinances.domain.repositories.MfaRecoveryCodeRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AuthService
import com.ynixt.sharedfinances.domain.services.mfa.MfaSecretCryptoService
import com.ynixt.sharedfinances.domain.services.mfa.MfaService
import com.ynixt.sharedfinances.domain.services.mfa.MfaSettingsService
import com.ynixt.sharedfinances.domain.services.mfa.TotpService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

@Service
class MfaSettingsServiceImpl(
    private val authService: AuthService,
    private val mfaEnrollmentRepository: MfaEnrollmentRepository,
    private val totpService: TotpService,
    private val mfaSecretCryptoService: MfaSecretCryptoService,
    private val userRepository: UserRepository,
    private val mfaService: MfaService,
    private val mfaRecoveryCodeRepository: MfaRecoveryCodeRepository,
    private val passwordEncoder: PasswordEncoder,
) : MfaSettingsService {
    override fun enableMfaBegin(
        userId: UUID,
        rawPassword: String,
    ): Mono<EnableMfaResponseDto> {
        return authService.checkPassword(userId, rawPassword).flatMap { user ->
            if (user.mfaEnabled) {
                return@flatMap Mono.error(MfaAlreadyEnabledException())
            }

            mfaEnrollmentRepository
                .deleteAllByUserId(userId)
                .then(
                    Mono.defer {
                        val secretBase32 = totpService.generateNewSecret()
                        val secretEnc = mfaSecretCryptoService.encryptTotpSecret(secretBase32)

                        mfaEnrollmentRepository
                            .save(
                                MfaEnrollmentEntity(
                                    userId = userId,
                                    secretEnc = secretEnc,
                                ),
                            ).map { saved ->
                                EnableMfaResponseDto(
                                    enrollmentId = saved.id!!,
                                    secretBase32 = secretBase32,
                                    otpauthUri =
                                        buildOtpauthUri(
                                            secretBase32 = secretBase32,
                                            email = user.email,
                                            issuer = "Shared Finances",
                                        ),
                                )
                            }
                    },
                )
        }
    }

    @Transactional
    override fun enableMfaConfirm(
        userId: UUID,
        enrollmentId: UUID,
        code: String,
    ): Mono<ConfirmMfaResponseDto> {
        val now = OffsetDateTime.now()

        return mfaEnrollmentRepository
            .consumeValidEnrollmentReturningSecret(
                id = enrollmentId,
                userId = userId,
                now = now,
            ).flatMap { secretEnc ->
                if (mfaService.decryptAndVerify(
                        secret = secretEnc,
                        code = code,
                    )
                ) {
                    userRepository
                        .enableMfa(
                            userId = userId,
                            totpSecret = secretEnc,
                        ).flatMap {
                            generateNewRecoveryCodes(userId).map { recoveryCodes ->
                                ConfirmMfaResponseDto(recoveryCodes = recoveryCodes)
                            }
                        }
                } else {
                    Mono.error(WrongMfaCodeException())
                }
            }.switchIfEmpty(Mono.error(MfaEnrollmentNotFoundException()))
    }

    override fun generateNewRecoveryCodes(userId: UUID): Mono<List<String>> {
        val now = OffsetDateTime.now()
        val rng = SecureRandom()

        val secretWords =
            (1..10).map {
                val bytes = ByteArray(12)
                rng.nextBytes(bytes)

                val raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

                raw.chunked(4).joinToString("-")
            }

        val entities =
            secretWords.map {
                MfaRecoveryCodeEntity(
                    userId = userId,
                    createdAt = now,
                    codeHash = passwordEncoder.encode(it)!!,
                )
            }

        return mfaRecoveryCodeRepository
            .deleteAllUnusedByUserId(userId)
            .thenMany(mfaRecoveryCodeRepository.saveAll(entities))
            .then(Mono.just(secretWords))
    }

    override fun disableMfa(
        userId: UUID,
        rawPassword: String,
        code: String,
    ): Mono<Unit> {
        return authService.checkPassword(userId, rawPassword).flatMap { user ->
            if (!user.mfaEnabled) {
                return@flatMap Mono.error(IllegalStateException("MFA already disabled"))
            }

            if (mfaService.decryptAndVerify(
                    secret = user.totpSecret!!,
                    code = code,
                )
            ) {
                mfaRecoveryCodeRepository
                    .deleteAllUnusedByUserId(userId)
                    .then(
                        userRepository.disableMfa(userId).map { },
                    )
            } else {
                Mono.error(WrongMfaCodeException())
            }
        }
    }

    private fun buildOtpauthUri(
        secretBase32: String,
        email: String,
        issuer: String,
    ): String {
        val label = "$issuer:$email"
        return "otpauth://totp/${encode(label)}?secret=${encode(secretBase32)}&issuer=${encode(issuer)}&digits=6&period=30"
    }

    private fun encode(v: String): String = URLEncoder.encode(v, StandardCharsets.UTF_8)
}
