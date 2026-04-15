package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.SESSION_CLAIM_NAME
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.Base64
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class AuthRegistrationAndSessionIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var redisTemplate: ReactiveRedisTemplate<String, String>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var userTestUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        userTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
    }

    @Test
    fun `login persists session in redis with about one month ttl`() =
        runBlocking {
            userTestUtil.createUserOnDatabase()
            val authorization = userTestUtil.login()
            val sessionId = sessionIdFromJwt(authorization)
            val key = "sf:auth:session:$sessionId"
            val ttl = redisTemplate.getExpire(key).awaitSingle()
            assertThat(ttl.seconds).isBetween(25L * 86400, 35L * 86400)
        }

    @Test
    fun `register rejects missing terms consent`() {
        webClient
            .post()
            .uri("/open/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "email" to "reg-${UUID.randomUUID()}@example.com",
                    "password" to "secret12",
                    "firstName" to "A",
                    "lastName" to "B",
                    "lang" to "en",
                    "defaultCurrency" to "USD",
                    "tmz" to "UTC",
                    "acceptTerms" to false,
                    "acceptPrivacy" to true,
                    "gravatarOptIn" to false,
                ),
            ).exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `register persists legal metadata and optional gravatar opt out`() =
        runBlocking {
            val email = "reg-${UUID.randomUUID()}@example.com"
            webClient
                .post()
                .uri("/open/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "email" to email,
                        "password" to "secret12",
                        "firstName" to "A",
                        "lastName" to "B",
                        "lang" to "en",
                        "defaultCurrency" to "USD",
                        "tmz" to "UTC",
                        "acceptTerms" to true,
                        "acceptPrivacy" to true,
                        "gravatarOptIn" to false,
                    ),
                ).exchange()
                .expectStatus()
                .isOk

            val user = userRepository.findOneByEmail(email).awaitSingle()
            assertThat(user.termsAcceptedAt).isNotNull
            assertThat(user.privacyAcceptedAt).isNotNull
            assertThat(user.termsVersion).isEqualTo("test-terms-1")
            assertThat(user.privacyVersion).isEqualTo("test-privacy-1")
            assertThat(user.photoUrl).isNull()
        }

    @Test
    fun `deleting current account clears redis session and refresh index for user`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val userId = user.id!!
            val authorization = userTestUtil.login()
            val sessionId = sessionIdFromJwt(authorization)
            val sessionKey = "sf:auth:session:$sessionId"
            val userSessionsKey = "sf:auth:user:$userId:sessions"
            val refreshIndexKey = "sf:auth:session:$sessionId:rth"

            assertThat(redisTemplate.hasKey(sessionKey).awaitSingle()).isTrue()
            assertThat(redisTemplate.opsForSet().size(userSessionsKey).awaitSingle()).isGreaterThan(0L)

            webClient
                .delete()
                .uri("/users/current")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .exchange()
                .expectStatus()
                .isNoContent

            assertThat(redisTemplate.hasKey(sessionKey).awaitSingle()).isFalse()
            assertThat(redisTemplate.hasKey(userSessionsKey).awaitSingle()).isFalse()
            assertThat(redisTemplate.hasKey(refreshIndexKey).awaitSingle()).isFalse()
        }

    private fun sessionIdFromJwt(authorization: String): UUID {
        val token = authorization.removePrefix("Bearer ").trim()
        val parts = token.split(".")
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
        val claims = objectMapper.readValue<Map<String, Any?>>(payloadJson)
        val session = claims[SESSION_CLAIM_NAME] as String
        return UUID.fromString(session)
    }
}
