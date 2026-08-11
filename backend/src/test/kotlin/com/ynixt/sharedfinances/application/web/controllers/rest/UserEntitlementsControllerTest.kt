package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.plan.UserEntitlements
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal
import com.ynixt.sharedfinances.domain.services.OnboardingService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.plan.PlanEntitlementsService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class UserEntitlementsControllerTest {
    @Test
    fun `endpoint always queries the authenticated principal`() =
        runTest {
            val authenticatedUserId = UUID.randomUUID()
            val entitlements = RecordingEntitlements()
            val controller =
                UserController(
                    userService = Mockito.mock(UserService::class.java),
                    userDtoMapper = Mockito.mock(UserDtoMapper::class.java),
                    onboardingService = Mockito.mock(OnboardingService::class.java),
                    planEntitlementsService = entitlements,
                )

            val response = controller.currentEntitlements(token(authenticatedUserId))

            assertEquals(authenticatedUserId, entitlements.requestedUserId)
            assertEquals(UserPlanRole.USER, response.role)
            assertEquals(true, response.limitsEnabled)
        }

    private fun token(userId: UUID): UserJwtAuthenticationToken {
        val principal =
            UserPrincipal(
                id = userId,
                email = "current@example.com",
                firstName = "Current",
                lastName = "User",
                lang = "en-US",
                defaultCurrency = "USD",
                tmz = "Pacific/Honolulu",
                photoUrl = null,
                emailVerified = true,
                mfaEnabled = false,
                onboardingDone = true,
                darkMode = false,
                role = UserPlanRole.USER,
                lastLoginAt = OffsetDateTime.ofInstant(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC),
                authorities = emptyList(),
            )
        val jwt =
            Jwt
                .withTokenValue("test")
                .header("alg", "none")
                .subject(userId.toString())
                .issuedAt(Instant.parse("2026-08-09T00:00:00Z"))
                .expiresAt(Instant.parse("2026-08-10T00:00:00Z"))
                .build()
        return UserJwtAuthenticationToken(jwt, principal, emptyList())
    }

    private class RecordingEntitlements : PlanEntitlementsService {
        var requestedUserId: UUID? = null

        override suspend fun get(
            userId: UUID,
            role: UserPlanRole,
            lastLoginAt: OffsetDateTime,
        ): UserEntitlements {
            requestedUserId = userId
            return UserEntitlements(limitsEnabled = true, role = role, importMaxLines = 100, quotas = emptyList())
        }
    }
}
