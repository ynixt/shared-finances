package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.auth.LoginResultDto
import com.ynixt.sharedfinances.application.web.dto.auth.mfa.EnableMfaResponseDto
import com.ynixt.sharedfinances.domain.entities.mfa.MfaChallengeEntity
import com.ynixt.sharedfinances.domain.entities.mfa.MfaEnrollmentEntity
import com.ynixt.sharedfinances.domain.repositories.MfaChallengeRepository
import com.ynixt.sharedfinances.domain.repositories.MfaEnrollmentRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.mfa.MfaSecretCryptoService
import com.ynixt.sharedfinances.domain.services.mfa.TotpService
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.binary.Base32
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.nio.ByteBuffer
import java.time.OffsetDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class AuthMfaRedisIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var redisTemplate: org.springframework.data.redis.core.ReactiveRedisTemplate<String, String>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var totpService: TotpService

    @Autowired
    private lateinit var mfaSecretCryptoService: MfaSecretCryptoService

    @Autowired
    private lateinit var mfaChallengeRepository: MfaChallengeRepository

    @Autowired
    private lateinit var mfaEnrollmentRepository: MfaEnrollmentRepository

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
    fun `mfa login stores challenge in redis with ttl and mfa endpoint consumes it once`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val rawSecret = totpService.generateNewSecret()
            val enc = mfaSecretCryptoService.encryptTotpSecret(rawSecret)
            userRepository.enableMfa(userId = user.id!!, totpSecret = enc).awaitSingle()

            val loginJson =
                webClient
                    .post()
                    .uri("/open/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        mapOf(
                            "email" to user.email,
                            "password" to "pass123",
                        ),
                    ).exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!
            val loginResult = objectMapper.readValue<LoginResultDto>(loginJson)
            assertThat(loginResult.mfaChallengeId).isNotNull
            val challengeId = loginResult.mfaChallengeId!!

            val challengeKey = "sf:auth:mfa:challenge:$challengeId"
            val ttl = redisTemplate.getExpire(challengeKey).awaitSingle()
            assertThat(ttl.seconds).isBetween(1L, 5L * 60)

            val code = totpCode(rawSecret)
            webClient
                .post()
                .uri("/open/auth/mfa")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "challengeId" to challengeId,
                        "code" to code,
                    ),
                ).exchange()
                .expectStatus()
                .isNoContent
                .expectHeader()
                .exists(HttpHeaders.AUTHORIZATION)

            assertThat(redisTemplate.hasKey(challengeKey).awaitSingle()).isFalse

            webClient
                .post()
                .uri("/open/auth/mfa")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "challengeId" to challengeId,
                        "code" to code,
                    ),
                ).exchange()
                .expectStatus()
                .isUnauthorized
        }

    @Test
    fun `wrong mfa code does not invalidate challenge so a later correct code succeeds`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val rawSecret = totpService.generateNewSecret()
            val enc = mfaSecretCryptoService.encryptTotpSecret(rawSecret)
            userRepository.enableMfa(userId = user.id!!, totpSecret = enc).awaitSingle()

            val loginJson =
                webClient
                    .post()
                    .uri("/open/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        mapOf(
                            "email" to user.email,
                            "password" to "pass123",
                        ),
                    ).exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!
            val challengeId = objectMapper.readValue<LoginResultDto>(loginJson).mfaChallengeId!!
            val challengeKey = "sf:auth:mfa:challenge:$challengeId"

            webClient
                .post()
                .uri("/open/auth/mfa")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "challengeId" to challengeId,
                        "code" to "000000",
                    ),
                ).exchange()
                .expectStatus()
                .isUnauthorized

            assertThat(redisTemplate.hasKey(challengeKey).awaitSingle()).isTrue

            val code = totpCode(rawSecret)
            webClient
                .post()
                .uri("/open/auth/mfa")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "challengeId" to challengeId,
                        "code" to code,
                    ),
                ).exchange()
                .expectStatus()
                .isNoContent
                .expectHeader()
                .exists(HttpHeaders.AUTHORIZATION)
        }

    @Test
    fun `mfa challenge consume is single use at repository layer`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val challenge =
                mfaChallengeRepository
                    .save(
                        MfaChallengeEntity(
                            userId = user.id!!,
                            userAgent = null,
                            ip = null,
                        ),
                    ).awaitSingle()
            val id = challenge.id!!
            val now = OffsetDateTime.now()
            val first = mfaChallengeRepository.consumeChallengeReturningUserId(id, now).awaitSingleOrNull()
            assertThat(first).isEqualTo(user.id)
            val second = mfaChallengeRepository.consumeChallengeReturningUserId(id, now).awaitSingleOrNull()
            assertThat(second).isNull()
        }

    @Test
    fun `mfa enrollment begin replaces pending enrollment and confirm consumes once`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val token = userTestUtil.login()

            fun begin(): EnableMfaResponseDto =
                webClient
                    .post()
                    .uri("/mfa-settings/begin")
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mapOf("rawPassword" to "pass123"))
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!
                    .let { objectMapper.readValue<EnableMfaResponseDto>(it) }

            val first = begin()
            val firstKey = "sf:auth:mfa:enrollment:${first.enrollmentId}"
            assertThat(redisTemplate.hasKey(firstKey).awaitSingle()).isTrue

            val second = begin()
            assertThat(second.enrollmentId).isNotEqualTo(first.enrollmentId)
            assertThat(redisTemplate.hasKey(firstKey).awaitSingle()).isFalse
            val secondKey = "sf:auth:mfa:enrollment:${second.enrollmentId}"
            assertThat(redisTemplate.hasKey(secondKey).awaitSingle()).isTrue

            val code = totpCode(second.secretBase32)
            webClient
                .post()
                .uri("/mfa-settings/confirm")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "enrollmentId" to second.enrollmentId,
                        "code" to code,
                    ),
                ).exchange()
                .expectStatus()
                .isOk

            assertThat(redisTemplate.hasKey(secondKey).awaitSingle()).isFalse

            webClient
                .post()
                .uri("/mfa-settings/confirm")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "enrollmentId" to second.enrollmentId,
                        "code" to code,
                    ),
                ).exchange()
                .expectStatus()
                .isBadRequest
        }

    @Test
    fun `wrong enrollment totp does not remove enrollment so a later correct code succeeds`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val token = userTestUtil.login()

            val begin =
                webClient
                    .post()
                    .uri("/mfa-settings/begin")
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mapOf("rawPassword" to "pass123"))
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!
                    .let { objectMapper.readValue<EnableMfaResponseDto>(it) }

            val enrollmentKey = "sf:auth:mfa:enrollment:${begin.enrollmentId}"

            webClient
                .post()
                .uri("/mfa-settings/confirm")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "enrollmentId" to begin.enrollmentId,
                        "code" to "000000",
                    ),
                ).exchange()
                .expectStatus()
                .isUnauthorized

            assertThat(redisTemplate.hasKey(enrollmentKey).awaitSingle()).isTrue

            val code = totpCode(begin.secretBase32)
            webClient
                .post()
                .uri("/mfa-settings/confirm")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "enrollmentId" to begin.enrollmentId,
                        "code" to code,
                    ),
                ).exchange()
                .expectStatus()
                .isOk

            assertThat(redisTemplate.hasKey(enrollmentKey).awaitSingle()).isFalse
        }

    @Test
    fun `expired mfa challenge is rejected after ttl`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val expiresAt = OffsetDateTime.now().plusSeconds(2)
            val challenge =
                mfaChallengeRepository
                    .save(
                        MfaChallengeEntity(
                            userId = user.id!!,
                            userAgent = null,
                            ip = null,
                            createdAt = OffsetDateTime.now(),
                            expiresAt = expiresAt,
                        ),
                    ).awaitSingle()
            val id = challenge.id!!
            Thread.sleep(2500)
            val uid =
                mfaChallengeRepository
                    .consumeChallengeReturningUserId(id, OffsetDateTime.now())
                    .awaitSingleOrNull()
            assertThat(uid).isNull()
        }

    @Test
    fun `expired mfa enrollment is rejected after ttl`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val expiresAt = OffsetDateTime.now().plusSeconds(2)
            val enc = mfaSecretCryptoService.encryptTotpSecret(totpService.generateNewSecret())
            val enrollment =
                mfaEnrollmentRepository
                    .save(
                        MfaEnrollmentEntity(
                            userId = user.id!!,
                            secretEnc = enc,
                            createdAt = OffsetDateTime.now(),
                            expiresAt = expiresAt,
                        ),
                    ).awaitSingle()
            val id = enrollment.id!!
            Thread.sleep(2500)
            val secret =
                mfaEnrollmentRepository
                    .consumeValidEnrollmentReturningSecret(
                        id = id,
                        userId = user.id!!,
                        now = OffsetDateTime.now(),
                    ).awaitSingleOrNull()
            assertThat(secret).isNull()
        }

    @Test
    fun `concurrent mfa verification allows at most one success for same challenge`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val rawSecret = totpService.generateNewSecret()
            val enc = mfaSecretCryptoService.encryptTotpSecret(rawSecret)
            userRepository.enableMfa(userId = user.id!!, totpSecret = enc).awaitSingle()

            val loginJson =
                webClient
                    .post()
                    .uri("/open/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        mapOf(
                            "email" to user.email,
                            "password" to "pass123",
                        ),
                    ).exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!
            val challengeId = objectMapper.readValue<LoginResultDto>(loginJson).mfaChallengeId!!
            val code = totpCode(rawSecret)
            val body =
                mapOf(
                    "challengeId" to challengeId,
                    "code" to code,
                )

            val results =
                (1..2)
                    .map {
                        async {
                            webClient
                                .post()
                                .uri("/open/auth/mfa")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(body)
                                .exchange()
                                .returnResult(String::class.java)
                                .status
                                .value()
                        }
                    }.awaitAll()

            val successes = results.count { it == 204 }
            val failures = results.count { it == 401 }
            assertThat(successes).isEqualTo(1)
            assertThat(failures).isEqualTo(1)
        }

    private fun totpCode(rawBase32Secret: String): String {
        val base32 = Base32()
        val secretBytes = base32.decode(rawBase32Secret)
        val currentInterval =
            java.time.Instant
                .now()
                .epochSecond / 30
        val data = ByteBuffer.allocate(8).putLong(currentInterval).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 0xf
        val binary =
            ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        return String.format("%06d", binary % 1_000_000)
    }
}
