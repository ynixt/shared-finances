package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.resources.repositories.redis.EmailVerificationTokenRedisRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.mocks.UserEntityMock
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "app.auth.features.email-confirmation-enabled=true",
    ],
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class AuthEmailWorkflowsIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var emailVerificationTokenRedisRepository: EmailVerificationTokenRedisRepository

    @Test
    fun `second forgot-password request for same address returns 429 while cooldown active`() {
        val email = "cooldown-${UUID.randomUUID()}@example.com"
        val body =
            mapOf(
                "email" to email,
            )
        webClient
            .post()
            .uri("/open/auth/forgot-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk

        webClient
            .post()
            .uri("/open/auth/resend-forgot-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isEqualTo(429)
    }

    @Test
    fun `login returns 401 when email is not verified and confirmation is enabled`() =
        runBlocking {
            val email = "unverified-${UUID.randomUUID()}@example.com"
            val rawPassword = "secret12"
            userRepository
                .insert(
                    UserEntityMock.defaultUser(
                        email = email,
                        passwordHash = passwordEncoder.encode(rawPassword),
                        emailVerified = false,
                    ),
                ).awaitSingle()

            webClient
                .post()
                .uri("/open/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "email" to email,
                        "password" to rawPassword,
                    ),
                ).exchange()
                .expectStatus()
                .isUnauthorized
                .expectBody()
                .jsonPath("$.messageI18n")
                .isEqualTo("apiErrors.auth.emailNotVerified")
        }

    @Test
    fun `confirm-email verifies address and allows login when confirmation is enabled`() =
        runBlocking {
            val email = "confirm-${UUID.randomUUID()}@example.com"
            val rawPassword = "secret12"
            val user =
                userRepository
                    .insert(
                        UserEntityMock.defaultUser(
                            email = email,
                            passwordHash = passwordEncoder.encode(rawPassword),
                            emailVerified = false,
                        ),
                    ).awaitSingle()
            val userId = user.id!!
            val rawToken =
                emailVerificationTokenRedisRepository
                    .issueToken(
                        userId,
                        email,
                        Duration.ofMinutes(180),
                    ).awaitSingle()

            webClient
                .post()
                .uri("/open/auth/confirm-email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapOf("token" to rawToken))
                .exchange()
                .expectStatus()
                .isNoContent

            val loginResponse =
                webClient
                    .post()
                    .uri("/open/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        mapOf(
                            "email" to email,
                            "password" to rawPassword,
                        ),
                    ).exchange()
                    .expectStatus()
                    .isOk
                    .expectHeader()
                    .exists(HttpHeaders.AUTHORIZATION)
                    .returnResult()
            val authorization = loginResponse.responseHeaders.getFirst(HttpHeaders.AUTHORIZATION)
            assertThat(authorization).isNotBlank()
        }

    @Test
    fun `GET open auth preferences returns configured feature flags`() {
        webClient
            .get()
            .uri("/open/auth/preferences")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.emailConfirmationEnabled")
            .isEqualTo(true)
            .jsonPath("$.passwordRecoveryEnabled")
            .isEqualTo(true)
            .jsonPath("$.turnstileEnabled")
            .isEqualTo(false)
    }
}
