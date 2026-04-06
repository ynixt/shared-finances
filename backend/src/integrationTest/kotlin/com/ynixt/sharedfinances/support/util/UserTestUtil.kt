package com.ynixt.sharedfinances.support.util

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.support.mocks.UserEntityMock
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.util.UUID
import kotlin.test.assertNotNull

class UserTestUtil(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val webClient: WebTestClient,
    private val email: String = "credit-card-it-${UUID.randomUUID()}@example.com",
    private val rawPassword: String = "pass123",
) {
    fun createUserOnDatabase(): UserEntity =
        runBlocking {
            userRepository
                .insert(
                    UserEntityMock.defaultUser(
                        email = email,
                        passwordHash = passwordEncoder.encode(rawPassword),
                    ),
                ).awaitSingle()
        }

    fun login(): String {
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
                .returnResult<String>()

        val accessToken = loginResponse.responseHeaders.getFirst(HttpHeaders.AUTHORIZATION)
        assertNotNull(accessToken)

        return accessToken
    }
}
