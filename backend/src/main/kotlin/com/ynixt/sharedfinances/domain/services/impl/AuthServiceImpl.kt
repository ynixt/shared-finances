package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.RefreshTokenEntity
import com.ynixt.sharedfinances.domain.entities.SessionEntity
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.exceptions.MfaIsNeededException
import com.ynixt.sharedfinances.domain.exceptions.http.AccountTemporaryBlocked
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidCredentialsException
import com.ynixt.sharedfinances.domain.models.LoginResult
import com.ynixt.sharedfinances.domain.repositories.FailedLoginRepository
import com.ynixt.sharedfinances.domain.repositories.RefreshTokenRepository
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AuthService
import com.ynixt.sharedfinances.domain.services.SESSION_CLAIM_NAME
import com.ynixt.sharedfinances.domain.services.mfa.MfaService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import java.net.InetAddress
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val sessionRepository: SessionRepository,
    private val failedLoginRepository: FailedLoginRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtEncoder: JwtEncoder,
    private val mfaService: MfaService,
    @param:Value("\${app.security.jwt.kid}") private val kid: String,
    @param:Value("\${app.security.jwt.issuer}") private val issuer: String,
    @param:Value("\${app.security.jwt.accessTokenTTLMinutes}") private val accessTtlMinutes: Int,
    @param:Value("\${app.security.jwt.refreshTokenTTLMinutes}") private val refreshTtlMinutes: Int,
    @param:Value("\${app.security.wrongPasswordTtlMinutes}") private val wrongPasswordTtlMinutes: Int,
    @param:Value("\${app.security.wrongPasswordBlock}") private val wrongPasswordBlock: Int,
) : AuthService {
    val accessTtlSeconds = accessTtlMinutes * 60L
    val refreshTtlSeconds = refreshTtlMinutes * 60L
    val wrongPasswordTtlSeconds = wrongPasswordTtlMinutes * 60L

    override fun login(
        email: String,
        rawPassword: String,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<LoginResult> {
        val normalizedEmail = email.trim().lowercase()

        return refuseLoginIfTooManyFails(email = email, ip = ip).then(
            userRepository
                .findOneByEmail(normalizedEmail)
                .switchIfEmpty(
                    Mono.error(
                        InvalidCredentialsException(
                            email = normalizedEmail,
                            ip = ip?.toString(),
                        ),
                    ),
                ).flatMap { user ->
                    val correctHash =
                        user.passwordHash
                            ?: return@flatMap Mono.error(
                                InvalidCredentialsException(
                                    email = normalizedEmail,
                                    ip = ip?.toString(),
                                ),
                            )

                    if (!passwordEncoder.matches(rawPassword, correctHash)) {
                        return@flatMap Mono.error(
                            InvalidCredentialsException(
                                email = normalizedEmail,
                                ip = ip?.toString(),
                            ),
                        )
                    }

                    if (user.mfaEnabled) {
                        requestMfa(
                            user = user,
                            userAgent = userAgent,
                            ip = ip,
                        )
                    } else {
                        loginSuccess(
                            user = user,
                            userAgent = userAgent,
                            ip = ip,
                        )
                    }
                }.onErrorResume { error ->
                    if (error is InvalidCredentialsException && error.ip != null && error.email != null) {
                        failedLoginRepository
                            .incrementFails(
                                ip = error.ip,
                                email = error.email,
                                ttlSeconds = wrongPasswordTtlSeconds,
                            ).then(Mono.error(error))
                    } else {
                        Mono.error(error)
                    }
                },
        )
    }

    override fun mfa(
        challengeId: UUID,
        code: String,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<LoginResult> =
        mfaService
            .verifyChallenge(
                challengeId = challengeId,
                code = code,
                ip = ip,
            ).flatMap { user ->
                refuseLoginIfTooManyFails(email = user.email, ip = ip).then(
                    loginSuccess(
                        user = user,
                        userAgent = userAgent,
                        ip = ip,
                    ),
                )
            }.onErrorResume { error ->
                if (error is InvalidCredentialsException && error.email != null && error.ip != null) {
                    failedLoginRepository
                        .incrementFails(
                            ip = error.ip,
                            email = error.email,
                            ttlSeconds = wrongPasswordTtlSeconds,
                        ).then(Mono.error(error))
                } else {
                    Mono.error(error)
                }
            }

    override fun logout(session: UUID): Mono<Void> = sessionRepository.deleteById(session).then().onErrorResume { Mono.empty() }

    override fun refreshToken(refreshToken: String): Mono<String> {
        val hash = sha256(refreshToken)

        return refreshTokenRepository
            .findByTokenHashAndExpiresAtAfter(hash, Instant.now())
            .flatMap { refreshTokenResult ->
                // TODO: less selects
                sessionRepository.findById(refreshTokenResult.sessionId).flatMap { session ->
                    userRepository.findById(session.userId).flatMap { user -> mintAccessToken(user, session.id!!) }
                }
            }.switchIfEmpty {
                Mono.error(BadCredentialsException("invalid refresh token"))
            }
    }

    override fun checkPassword(
        userId: UUID,
        rawPassword: String,
    ): Mono<UserEntity> {
        return userRepository
            .findById(userId)
            .switchIfEmpty(Mono.error(BadCredentialsException("invalid credentials")))
            .flatMap { user ->
                val correctHash =
                    user.passwordHash
                        ?: return@flatMap Mono.error(BadCredentialsException("invalid credentials"))

                if (!passwordEncoder.matches(rawPassword, correctHash)) {
                    return@flatMap Mono.error(BadCredentialsException("invalid credentials"))
                }

                Mono.just(user)
            }
    }

    private fun requestMfa(
        user: UserEntity,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<LoginResult> =
        mfaService
            .generateNewChallenge(
                userId = user.id!!,
                userAgent = userAgent,
                ip = ip,
            ).flatMap { challengeId ->
                Mono.error(MfaIsNeededException(challengeId = challengeId))
            }

    private fun loginSuccess(
        user: UserEntity,
        userAgent: String?,
        ip: InetAddress?,
    ): Mono<LoginResult> {
        val refreshToken = generateOpaqueToken()
        val refreshTokenHash = sha256(refreshToken)

        val now = Instant.now()
        val refreshExpiresAt = now.plusSeconds(refreshTtlSeconds)

        return failedLoginRepository
            .deleteByIpAndEmail(
                ip = ip.toString(),
                email = user.email,
            ).then(
                sessionRepository
                    .save(
                        SessionEntity(
                            userId = user.id!!,
                            userAgent = userAgent,
                            ip = ip,
                        ),
                    ).flatMap { session ->
                        refreshTokenRepository
                            .save(
                                RefreshTokenEntity(
                                    sessionId = session.id!!,
                                    tokenHash = refreshTokenHash,
                                    createdAt = now,
                                    expiresAt = refreshExpiresAt,
                                ),
                            ).then(
                                mintAccessToken(user, session.id!!)
                                    .map { access ->
                                        LoginResult(
                                            accessToken = access,
                                            refreshToken = refreshToken,
                                            refreshExpiresInSeconds = refreshTtlSeconds,
                                        )
                                    },
                            )
                    },
            )
    }

    private fun mintAccessToken(
        user: UserEntity,
        session: UUID,
    ): Mono<String> {
        val now = Instant.now()

        val header =
            JwsHeader
                .with(SignatureAlgorithm.RS256)
                .type("JWT")
                .keyId(kid)
                .build()

        val claims =
            JwtClaimsSet
                .builder()
                .issuer(issuer)
                .subject(user.id.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTtlSeconds))
                .id(UUID.randomUUID().toString())
                .claim("email", user.email)
                .claim(SESSION_CLAIM_NAME, session)
                // .claim("roles", user.roles)
                .build()

        return Mono
            .fromCallable {
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
            }.subscribeOn(Schedulers.boundedElastic())
    }

    private fun generateOpaqueToken(bytes: Int = 64): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }

    private fun sha256(input: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))

    private fun refuseLoginIfTooManyFails(
        email: String,
        ip: InetAddress?,
    ): Mono<Void> =
        failedLoginRepository.getFails(ip.toString(), email).flatMap { fails ->
            if (fails > wrongPasswordBlock) {
                Mono.error(AccountTemporaryBlocked())
            } else {
                Mono.empty()
            }
        }
}
