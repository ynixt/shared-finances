package com.ynixt.sharedfinances.resources.services.auth

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.exceptions.http.EmailAlreadyInUseException
import com.ynixt.sharedfinances.domain.exceptions.http.auth.AuthTokenInvalidOrExpiredException
import com.ynixt.sharedfinances.domain.exceptions.http.auth.EmailConfirmationDisabledException
import com.ynixt.sharedfinances.domain.exceptions.http.auth.EmailResendCooldownActiveException
import com.ynixt.sharedfinances.domain.exceptions.http.auth.PasswordRecoveryDisabledException
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.auth.OpenAuthEmailWorkflowService
import com.ynixt.sharedfinances.resources.repositories.redis.AuthRedisKeys
import com.ynixt.sharedfinances.resources.repositories.redis.AuthResendCooldownRedisRepository
import com.ynixt.sharedfinances.resources.repositories.redis.EmailVerificationTokenRedisRepository
import com.ynixt.sharedfinances.resources.repositories.redis.PasswordResetTokenRedisRepository
import com.ynixt.sharedfinances.resources.services.mail.AuthTransactionalMailMessageComposer
import com.ynixt.sharedfinances.resources.services.mail.TransactionalEmailDispatchService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.Locale

@Service
class OpenAuthEmailWorkflowServiceImpl(
    private val authProperties: AuthProperties,
    private val userRepository: UserRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val emailVerificationTokenRedisRepository: EmailVerificationTokenRedisRepository,
    private val passwordResetTokenRedisRepository: PasswordResetTokenRedisRepository,
    private val authResendCooldownRedisRepository: AuthResendCooldownRedisRepository,
    private val authTransactionalMailMessageComposer: AuthTransactionalMailMessageComposer,
    private val transactionalEmailDispatchService: TransactionalEmailDispatchService,
    private val sessionRepository: SessionRepository,
    private val passwordEncoder: PasswordEncoder,
) : OpenAuthEmailWorkflowService {
    override suspend fun sendRegistrationConfirmationIfNeeded(user: UserEntity) {
        if (!authProperties.features.emailConfirmationEnabled) {
            return
        }

        val userId = user.id ?: return
        val ttl = Duration.ofMinutes(authProperties.emailConfirmation.ttlMinutes)
        val raw =
            emailVerificationTokenRedisRepository
                .issueToken(userId, user.email, ttl)
                .awaitSingle()

        dispatchConfirmationEmail(user.email, getUserLocale(user), raw)
        markEmailConfirmationResendCooldown(user.email)
    }

    override suspend fun confirmEmail(rawToken: String) {
        if (!authProperties.features.emailConfirmationEnabled) {
            throw EmailConfirmationDisabledException()
        }

        val payload =
            emailVerificationTokenRedisRepository
                .consumeRawToken(rawToken)
                .awaitSingleOrNull()
                ?: throw AuthTokenInvalidOrExpiredException("apiErrors.auth.emailConfirmationTokenInvalid")

        val user =
            userRepository
                .findById(payload.userId)
                .awaitSingleOrNull()
                ?: throw AuthTokenInvalidOrExpiredException("apiErrors.auth.emailConfirmationTokenInvalid")

        if (user.emailVerified) {
            emailVerificationTokenRedisRepository.deleteActivePointer(payload.userId).awaitSingleOrNull()
            return
        }

        if (user.email.lowercase() != payload.email.lowercase()) {
            throw AuthTokenInvalidOrExpiredException("apiErrors.auth.emailConfirmationTokenInvalid")
        }

        userRepository.markEmailVerifiedIfUnverified(payload.userId).awaitSingle()
        emailVerificationTokenRedisRepository.deleteActivePointer(payload.userId).awaitSingleOrNull()
    }

    override suspend fun resendConfirmationEmail(email: String): Long {
        if (!authProperties.features.emailConfirmationEnabled) {
            throw EmailConfirmationDisabledException()
        }

        val normalized = normalizeEmail(email)
        val cooldownKey = AuthRedisKeys.emailVerifyResendForEmail(normalized)

        enforceResendCooldown(cooldownKey)

        val user =
            userRepository
                .findOneByEmail(normalized)
                .awaitSingleOrNull()

        if (user == null || user.emailVerified) {
            markResendCooldown(cooldownKey)
            return authProperties.emailResend.cooldownSeconds
        }

        val userId = user.id!!
        val ttl = Duration.ofMinutes(authProperties.emailConfirmation.ttlMinutes)
        val raw =
            emailVerificationTokenRedisRepository
                .issueToken(userId, user.email, ttl)
                .awaitSingle()

        dispatchConfirmationEmail(user.email, getUserLocale(user), raw)
        markResendCooldown(cooldownKey)

        return authProperties.emailResend.cooldownSeconds
    }

    override suspend fun changePendingEmail(
        currentEmail: String,
        newEmail: String,
    ): Long {
        if (!authProperties.features.emailConfirmationEnabled) {
            throw EmailConfirmationDisabledException()
        }

        val cur = normalizeEmail(currentEmail)
        val next = normalizeEmail(newEmail)

        val user =
            userRepository
                .findOneByEmail(cur)
                .awaitSingleOrNull()
                ?: throw AuthTokenInvalidOrExpiredException("apiErrors.auth.pendingEmailChangeInvalid")

        if (user.emailVerified) {
            throw AuthTokenInvalidOrExpiredException("apiErrors.auth.pendingEmailChangeInvalid")
        }

        val userId = user.id!!

        enforceResendCooldown(AuthRedisKeys.emailVerifyResend(userId))

        userRepository
            .updateEmailWhenUnverified(userId, next)
            .onErrorMap { t ->
                if (databaseHelperService.isUniqueViolation(t, "users_email_key")) {
                    EmailAlreadyInUseException(next)
                } else {
                    t
                }
            }.awaitSingle()

        val updated =
            userRepository
                .findById(userId)
                .awaitSingle()
        val ttl = Duration.ofMinutes(authProperties.emailConfirmation.ttlMinutes)
        val raw =
            emailVerificationTokenRedisRepository
                .issueToken(userId, updated.email, ttl)
                .awaitSingle()

        dispatchConfirmationEmail(updated.email, getUserLocale(updated), raw)
        markResendCooldown(AuthRedisKeys.emailVerifyResend(userId))
        markEmailConfirmationResendCooldown(updated.email)

        return authProperties.emailResend.cooldownSeconds
    }

    override suspend fun requestPasswordReset(email: String): Long {
        if (!authProperties.features.passwordRecoveryEnabled) {
            throw PasswordRecoveryDisabledException()
        }

        val normalized = normalizeEmail(email)
        val cooldownKey = AuthRedisKeys.passwordResetResendForEmail(normalized)

        enforceResendCooldown(cooldownKey)
        val user =
            userRepository
                .findOneByEmail(normalized)
                .awaitSingleOrNull()

        if (user != null && user.passwordHash != null) {
            val userId = user.id!!
            val ttl = Duration.ofMinutes(authProperties.passwordRecovery.ttlMinutes)
            val raw =
                passwordResetTokenRedisRepository
                    .issueToken(userId, user.email, ttl)
                    .awaitSingle()
            dispatchPasswordResetEmail(user.email, getUserLocale(user), raw)
        }

        markResendCooldown(cooldownKey)

        return authProperties.emailResend.cooldownSeconds
    }

    override suspend fun resendPasswordResetEmail(email: String): Long = requestPasswordReset(email)

    override suspend fun confirmPasswordReset(
        rawToken: String,
        newPassword: String,
    ) {
        if (!authProperties.features.passwordRecoveryEnabled) {
            throw PasswordRecoveryDisabledException()
        }

        val payload =
            passwordResetTokenRedisRepository
                .consumeRawToken(rawToken)
                .awaitSingleOrNull()
                ?: throw AuthTokenInvalidOrExpiredException("apiErrors.auth.passwordResetTokenInvalid")

        val user =
            userRepository
                .findById(payload.userId)
                .awaitSingleOrNull()
                ?: throw AuthTokenInvalidOrExpiredException("apiErrors.auth.passwordResetTokenInvalid")

        if (user.email.lowercase() != payload.email.lowercase()) {
            throw AuthTokenInvalidOrExpiredException("apiErrors.auth.passwordResetTokenInvalid")
        }

        val hash = requireNotNull(passwordEncoder.encode(newPassword))

        userRepository.changePassword(payload.userId, hash).awaitSingle()
        sessionRepository.deleteAllByUserId(payload.userId).awaitSingle()
        passwordResetTokenRedisRepository.deleteActivePointer(payload.userId).awaitSingleOrNull()
    }

    private suspend fun dispatchConfirmationEmail(
        to: String,
        locale: Locale,
        rawToken: String,
    ) {
        transactionalEmailDispatchService.send(
            authTransactionalMailMessageComposer.buildEmailConfirmation(
                toAddress = to,
                locale = locale,
                rawToken = rawToken,
            ),
        )
    }

    private suspend fun dispatchPasswordResetEmail(
        to: String,
        locale: Locale,
        rawToken: String,
    ) {
        transactionalEmailDispatchService.send(
            authTransactionalMailMessageComposer.buildPasswordReset(
                toAddress = to,
                locale = locale,
                rawToken = rawToken,
            ),
        )
    }

    private suspend fun enforceResendCooldown(key: String) {
        val rem =
            authResendCooldownRedisRepository
                .remainingSeconds(key)
                .awaitSingle()

        if (rem > 0) {
            throw EmailResendCooldownActiveException(rem)
        }
    }

    private suspend fun markResendCooldown(key: String) {
        authResendCooldownRedisRepository
            .setCooldown(
                key,
                Duration.ofSeconds(authProperties.emailResend.cooldownSeconds),
            ).awaitSingle()
    }

    private suspend fun markEmailConfirmationResendCooldown(email: String) {
        markResendCooldown(AuthRedisKeys.emailVerifyResendForEmail(normalizeEmail(email)))
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun getUserLocale(user: UserEntity): Locale = Locale.forLanguageTag(user.lang)
}
