package com.ynixt.sharedfinances.application.converters

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.SESSION_CLAIM_NAME
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtToUserAuthConverterPlanRoleTest {
    @Test
    fun `authority follows the stored role for the same token`() {
        val userRepository = Mockito.mock(UserRepository::class.java)
        val sessionRepository = Mockito.mock(SessionRepository::class.java)
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val user = user(userId, UserPlanRole.USER)
        val jwt =
            Jwt
                .withTokenValue("unchanged-token")
                .header("alg", "none")
                .subject(userId.toString())
                .claim(SESSION_CLAIM_NAME, sessionId.toString())
                .build()
        Mockito.`when`(sessionRepository.existsById(sessionId)).thenReturn(Mono.just(true))
        Mockito.`when`(userRepository.findById(userId)).thenAnswer { Mono.just(user) }
        val converter = JwtToUserAuthConverter(userRepository, sessionRepository, Clock.systemUTC())

        assertEquals(
            setOf("ROLE_USER"),
            converter
                .convert(jwt)
                .block()!!
                .authorities
                .map { it.authority }
                .toSet(),
        )

        user.role = UserPlanRole.ADMINISTRATOR
        val updatedAuthorities =
            converter
                .convert(jwt)
                .block()!!
                .authorities
                .map { it.authority }
                .toSet()
        assertTrue("ROLE_USER" in updatedAuthorities)
        assertTrue("ROLE_ADMINISTRATOR" in updatedAuthorities)
    }

    @Test
    fun `authenticated request records stale activity and clears its warning`() {
        val userRepository = Mockito.mock(UserRepository::class.java)
        val sessionRepository = Mockito.mock(SessionRepository::class.java)
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val user = user(userId, UserPlanRole.USER)
        user.lastLoginAt = OffsetDateTime.parse("2026-01-01T00:00:00Z")
        user.inactivityNoticeStage = 30
        val usedAt = OffsetDateTime.parse("2026-01-02T00:00:00Z")
        val cutoff = usedAt.minusHours(1)
        val jwt =
            Jwt
                .withTokenValue("activity-token")
                .header("alg", "none")
                .subject(userId.toString())
                .claim(SESSION_CLAIM_NAME, sessionId.toString())
                .build()
        Mockito.`when`(sessionRepository.existsById(sessionId)).thenReturn(Mono.just(true))
        Mockito.`when`(userRepository.findById(userId)).thenReturn(Mono.just(user))
        Mockito.`when`(userRepository.recordActivityIfOlderThan(userId, usedAt, cutoff)).thenReturn(Mono.just(1))

        JwtToUserAuthConverter(
            userRepository,
            sessionRepository,
            Clock.fixed(Instant.parse("2026-01-02T00:00:00Z"), ZoneOffset.UTC),
        ).convert(jwt).block()

        Mockito.verify(userRepository).recordActivityIfOlderThan(userId, usedAt, cutoff)
        assertEquals(usedAt, user.lastLoginAt)
        assertEquals(null, user.inactivityNoticeStage)
    }

    private fun user(
        id: UUID,
        role: UserPlanRole,
    ) = UserEntity(
        email = "role@example.com",
        passwordHash = "hash",
        firstName = "Role",
        lastName = "User",
        lang = "en-US",
        defaultCurrency = "USD",
        tmz = "UTC",
        photoUrl = null,
        emailVerified = true,
        mfaEnabled = false,
        totpSecret = null,
        onboardingDone = true,
        role = role,
    ).also { it.id = id }
}
