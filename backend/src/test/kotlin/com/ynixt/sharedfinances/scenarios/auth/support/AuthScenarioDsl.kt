package com.ynixt.sharedfinances.scenarios.auth.support

import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import jakarta.validation.ConstraintViolationException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.net.InetAddress
import java.time.LocalDate
import java.util.UUID

fun authScenario(
    initialDate: LocalDate = LocalDate.of(2026, 1, 1),
    block: suspend AuthScenarioDsl.() -> Unit,
): AuthScenarioDsl =
    runBlocking {
        AuthScenarioDsl(initialDate).apply {
            block()
        }
    }

class AuthScenarioDsl(
    initialDate: LocalDate = LocalDate.of(2026, 1, 1),
) {
    private val runtime = AuthScenarioRuntime(initialDate)
    private val context = AuthScenarioContext()

    val given = Given(this)
    val whenActions = When(this)
    val then = Then(this)

    suspend fun given(block: suspend Given.() -> Unit): AuthScenarioDsl =
        chain {
            given.block()
        }

    suspend fun `when`(block: suspend When.() -> Unit): AuthScenarioDsl =
        chain {
            whenActions.block()
        }

    suspend fun then(block: suspend Then.() -> Unit): AuthScenarioDsl =
        chain {
            then.block()
        }

    private suspend fun chain(action: suspend () -> Unit): AuthScenarioDsl {
        action()
        return this
    }

    class Given internal constructor(
        private val dsl: AuthScenarioDsl,
    ) {
        suspend fun user(
            email: String = "user-${UUID.randomUUID()}@example.com",
            password: String = "pass123",
            firstName: String = "Scenario",
            lastName: String = "User",
            lang: String = "en",
            defaultCurrency: String = "USD",
            tmz: String = "UTC",
        ): UUID {
            val created =
                dsl.runtime.userService.createUser(
                    RegisterDto(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName,
                        lang = lang,
                        defaultCurrency = defaultCurrency,
                        tmz = tmz,
                        acceptTerms = true,
                        acceptPrivacy = true,
                        gravatarOptIn = false,
                    ),
                )

            val userId = requireNotNull(created.id)
            dsl.context.currentUserId = userId
            dsl.context.currentCurrency = created.defaultCurrency
            dsl.context.currentEmail = created.email
            dsl.context.currentPassword = password
            return userId
        }
    }

    class When internal constructor(
        private val dsl: AuthScenarioDsl,
    ) {
        suspend fun loginCurrentUser(
            userAgent: String? = "auth-scenario-test",
            ip: InetAddress? = InetAddress.getByName("127.0.0.1"),
        ) {
            val email = requireNotNull(dsl.context.currentEmail) { "Current user email is not configured" }
            val password = requireNotNull(dsl.context.currentPassword) { "Current user password is not configured" }

            val result = dsl.runtime.authService.login(email = email, rawPassword = password, userAgent = userAgent, ip = ip)
            dsl.context.lastError = null
            dsl.context.lastLoginResult = result
            dsl.context.currentSessionId = dsl.runtime.sessionIdFromAccessToken(result.accessToken)
        }

        suspend fun register(
            email: String = "reg-${UUID.randomUUID()}@example.com",
            password: String = "secret12",
            firstName: String = "A",
            lastName: String = "B",
            lang: String = "en",
            defaultCurrency: String = "USD",
            tmz: String = "UTC",
            acceptTerms: Boolean? = true,
            acceptPrivacy: Boolean? = true,
            gravatarOptIn: Boolean = false,
        ) {
            val request =
                RegisterDto(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    lang = lang,
                    defaultCurrency = defaultCurrency,
                    tmz = tmz,
                    acceptTerms = acceptTerms,
                    acceptPrivacy = acceptPrivacy,
                    gravatarOptIn = gravatarOptIn,
                )
            val violations = dsl.runtime.validateRegister(request)
            if (violations.isNotEmpty()) {
                throw ConstraintViolationException(violations)
            }

            val created = dsl.runtime.userService.createUser(request)
            dsl.context.lastError = null
            dsl.context.lastRegisterEmail = created.email
            dsl.context.currentUserId = created.id
            dsl.context.currentCurrency = created.defaultCurrency
        }

        suspend fun attemptRegister(
            email: String = "reg-${UUID.randomUUID()}@example.com",
            password: String = "secret12",
            firstName: String = "A",
            lastName: String = "B",
            lang: String = "en",
            defaultCurrency: String = "USD",
            tmz: String = "UTC",
            acceptTerms: Boolean? = true,
            acceptPrivacy: Boolean? = true,
            gravatarOptIn: Boolean = false,
        ) {
            dsl.context.lastError =
                runCatching {
                    register(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName,
                        lang = lang,
                        defaultCurrency = defaultCurrency,
                        tmz = tmz,
                        acceptTerms = acceptTerms,
                        acceptPrivacy = acceptPrivacy,
                        gravatarOptIn = gravatarOptIn,
                    )
                }.exceptionOrNull()
        }

        suspend fun deleteCurrentAccount() {
            val currentUserId = requireNotNull(dsl.context.currentUserId) { "Current user id is not configured" }
            dsl.runtime.userService.deleteCurrentAccount(currentUserId)
        }
    }

    class Then internal constructor(
        private val dsl: AuthScenarioDsl,
    ) {
        fun currentSessionTtlShouldBeBetweenDays(
            minimumDays: Long,
            maximumDays: Long,
        ) {
            val currentSessionId = requireNotNull(dsl.context.currentSessionId) { "Current session is not configured" }
            val authStore = dsl.runtime.infrastructure.authStore
            val ttlSeconds =
                authStore.getSessionTtlSeconds(currentSessionId)
                    ?: error("Session ttl was not found for $currentSessionId")
            assertThat(ttlSeconds).isBetween(minimumDays * 86400, maximumDays * 86400)
        }

        fun registrationShouldFailForMissingTermsConsent() {
            val error = dsl.context.lastError
            assertThat(error).isInstanceOf(ConstraintViolationException::class.java)
            val violation =
                (error as ConstraintViolationException)
                    .constraintViolations
                    .firstOrNull { it.propertyPath.toString() == "acceptTerms" }
            assertThat(violation).isNotNull()
        }

        suspend fun registeredUserShouldPersistLegalMetadata(
            email: String = requireNotNull(dsl.context.lastRegisterEmail) { "No user was registered in current scenario" },
            expectedTermsVersion: String = "test-terms-1",
            expectedPrivacyVersion: String = "test-privacy-1",
        ) {
            val user = dsl.runtime.loadUserByEmail(email)
            assertThat(user.termsAcceptedAt).isNotNull
            assertThat(user.privacyAcceptedAt).isNotNull
            assertThat(user.termsVersion).isEqualTo(expectedTermsVersion)
            assertThat(user.privacyVersion).isEqualTo(expectedPrivacyVersion)
        }

        suspend fun registeredUserPhotoShouldBeNull(
            email: String = requireNotNull(dsl.context.lastRegisterEmail) { "No user was registered in current scenario" },
        ) {
            val user = dsl.runtime.loadUserByEmail(email)
            assertThat(user.photoUrl).isNull()
        }

        fun currentSessionKeyShouldExist() {
            val currentSessionId = requireNotNull(dsl.context.currentSessionId) { "Current session is not configured" }
            val authStore = dsl.runtime.infrastructure.authStore
            assertThat(authStore.hasSessionKey(currentSessionId)).isTrue()
        }

        fun currentUserSessionIndexShouldExist() {
            val currentUserId = requireNotNull(dsl.context.currentUserId) { "Current user id is not configured" }
            val authStore = dsl.runtime.infrastructure.authStore
            assertThat(authStore.hasUserSessionsKey(currentUserId)).isTrue()
            assertThat(authStore.userSessionCount(currentUserId)).isGreaterThan(0L)
        }

        fun currentRefreshIndexShouldExist() {
            val currentSessionId = requireNotNull(dsl.context.currentSessionId) { "Current session is not configured" }
            val authStore = dsl.runtime.infrastructure.authStore
            assertThat(authStore.hasSessionRefreshIndexKey(currentSessionId)).isTrue()
        }

        fun currentSessionArtifactsShouldBeDeleted() {
            val currentSessionId = requireNotNull(dsl.context.currentSessionId) { "Current session is not configured" }
            val currentUserId = requireNotNull(dsl.context.currentUserId) { "Current user id is not configured" }
            val authStore = dsl.runtime.infrastructure.authStore

            assertThat(authStore.hasSessionKey(currentSessionId)).isFalse()
            assertThat(authStore.hasUserSessionsKey(currentUserId)).isFalse()
            assertThat(authStore.hasSessionRefreshIndexKey(currentSessionId)).isFalse()
        }
    }
}
