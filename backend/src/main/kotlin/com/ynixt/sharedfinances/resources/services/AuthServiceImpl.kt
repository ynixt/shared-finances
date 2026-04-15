package com.ynixt.sharedfinances.resources.services

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
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

    override suspend fun login(
        email: String,
        rawPassword: String,
        userAgent: String?,
        ip: InetAddress?,
    ): LoginResult {
        val normalizedEmail = email.trim().lowercase()

        refuseLoginIfTooManyFails(email = email, ip = ip)

        return try {
            val user =
                userRepository
                    .findOneByEmail(normalizedEmail)
                    .awaitSingleOrNull() ?: throw InvalidCredentialsException(
                    email = normalizedEmail,
                    ip = ip?.toString(),
                )

            val correctHash =
                user.passwordHash
                    ?: throw InvalidCredentialsException(
                        email = normalizedEmail,
                        ip = ip?.toString(),
                    )

            if (!passwordEncoder.matches(rawPassword, correctHash)) {
                throw InvalidCredentialsException(
                    email = normalizedEmail,
                    ip = ip?.toString(),
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
        } catch (error: InvalidCredentialsException) {
            if (error.ip != null && error.email != null) {
                failedLoginRepository
                    .incrementFails(
                        ip = error.ip,
                        email = error.email,
                        ttlSeconds = wrongPasswordTtlSeconds,
                    ).awaitSingleOrNull()
            }

            throw error
        }
    }

    override suspend fun mfa(
        challengeId: UUID,
        code: String,
        userAgent: String?,
        ip: InetAddress?,
    ): LoginResult =
        try {
            val user =
                mfaService
                    .verifyChallenge(
                        challengeId = challengeId,
                        code = code,
                        ip = ip,
                    )

            refuseLoginIfTooManyFails(email = user.email, ip = ip)

            loginSuccess(
                user = user,
                userAgent = userAgent,
                ip = ip,
            )
        } catch (error: InvalidCredentialsException) {
            if (error.email != null && error.ip != null) {
                failedLoginRepository
                    .incrementFails(
                        ip = error.ip,
                        email = error.email,
                        ttlSeconds = wrongPasswordTtlSeconds,
                    ).awaitSingle()
            }

            throw error
        }

    override suspend fun logout(session: UUID) {
        sessionRepository.deleteById(session).awaitSingle()
    }

    override suspend fun refreshToken(refreshToken: String): String {
        val hash = sha256(refreshToken)

        val refreshTokenResult =
            refreshTokenRepository
                .findByTokenHashAndExpiresAtAfter(hash, Instant.now())
                .awaitSingle()

        // TODO: less selects
        return sessionRepository.findById(refreshTokenResult.sessionId).awaitSingleOrNull()?.let { session ->
            userRepository.findById(session.userId).awaitSingleOrNull()?.let { user -> mintAccessToken(user, session.id!!) }
        } ?: throw BadCredentialsException("invalid refresh token")
    }

    override suspend fun checkPassword(
        userId: UUID,
        rawPassword: String,
    ): UserEntity {
        return userRepository
            .findById(userId)
            .awaitSingleOrNull()
            ?.let { user ->
                val correctHash = user.passwordHash ?: return@let null

                if (!passwordEncoder.matches(rawPassword, correctHash)) {
                    return@let null
                }

                user
            } ?: throw BadCredentialsException("invalid credentials")
    }

    private suspend fun requestMfa(
        user: UserEntity,
        userAgent: String?,
        ip: InetAddress?,
    ): LoginResult =
        mfaService
            .generateNewChallenge(
                userId = user.id!!,
                userAgent = userAgent,
                ip = ip,
            ).let { challengeId ->
                throw MfaIsNeededException(challengeId = challengeId)
            }

    private suspend fun loginSuccess(
        user: UserEntity,
        userAgent: String?,
        ip: InetAddress?,
    ): LoginResult {
        val refreshToken = generateOpaqueToken()
        val refreshTokenHash = sha256(refreshToken)

        val now = Instant.now()
        val refreshExpiresAt = now.plusSeconds(refreshTtlSeconds)

        failedLoginRepository
            .deleteByIpAndEmail(
                ip = ip.toString(),
                email = user.email,
            ).awaitSingleOrNull()

        return sessionRepository
            .save(
                SessionEntity(
                    userId = user.id!!,
                    userAgent = userAgent,
                    ip = ip,
                ),
            ).awaitSingle()
            .let { session ->
                refreshTokenRepository
                    .save(
                        RefreshTokenEntity(
                            sessionId = session.id!!,
                            tokenHash = refreshTokenHash,
                            createdAt = now,
                            expiresAt = refreshExpiresAt,
                        ),
                    ).awaitSingle()

                LoginResult(
                    accessToken = mintAccessToken(user, session.id!!),
                    refreshToken = refreshToken,
                    refreshExpiresInSeconds = refreshTtlSeconds,
                )
            }
    }

    private suspend fun mintAccessToken(
        user: UserEntity,
        session: UUID,
    ): String {
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

        return withContext(Dispatchers.IO) {
            jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        }
    }

    private fun generateOpaqueToken(bytes: Int = 64): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }

    private fun sha256(input: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))

    private suspend fun refuseLoginIfTooManyFails(
        email: String,
        ip: InetAddress?,
    ) = failedLoginRepository.getFails(ip.toString(), email).awaitSingle().let { fails ->
        if (fails > wrongPasswordBlock) {
            throw AccountTemporaryBlocked()
        }
    }
}
