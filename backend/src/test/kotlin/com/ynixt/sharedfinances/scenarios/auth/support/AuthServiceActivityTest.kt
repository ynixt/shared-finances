package com.ynixt.sharedfinances.scenarios.auth.support

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidCredentialsException
import com.ynixt.sharedfinances.domain.services.mfa.MfaService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AuthServiceActivityTest {
    @Test
    fun `successful sign-in records activity with the plan model disabled and clears warning stage`() =
        runTest {
            val runtime = AuthScenarioRuntime()
            val user = runtime.insertUser(lastLoginAt = "2025-12-01T00:00:00Z", noticeStage = 30)

            runtime.authService.login(user.email, PASSWORD, "test", LOOPBACK)

            assertEquals(1, runtime.infrastructure.userRepository.activityWrites)
            assertEquals(OffsetDateTime.parse("2026-01-01T00:00:00Z"), user.lastLoginAt)
            assertNull(user.inactivityNoticeStage)
        }

    @Test
    fun `rejected sign-in leaves activity untouched`() =
        runTest {
            val runtime = AuthScenarioRuntime()
            val user = runtime.insertUser(lastLoginAt = "2025-12-01T00:00:00Z", noticeStage = 30)

            assertFailsWith<InvalidCredentialsException> {
                runtime.authService.login(user.email, "wrong", "test", LOOPBACK)
            }

            assertEquals(0, runtime.infrastructure.userRepository.activityWrites)
            assertEquals(OffsetDateTime.parse("2025-12-01T00:00:00Z"), user.lastLoginAt)
            assertEquals(30, user.inactivityNoticeStage)
        }

    @Test
    fun `refresh records activity and writes again only after throttle interval`() =
        runTest {
            val runtime = AuthScenarioRuntime()
            val user = runtime.insertUser(lastLoginAt = "2025-12-01T00:00:00Z")
            val login = runtime.authService.login(user.email, PASSWORD, "test", LOOPBACK)

            runtime.authService.login(user.email, PASSWORD, "test", LOOPBACK)
            assertEquals(1, runtime.infrastructure.userRepository.activityWrites)

            runtime.infrastructure.clock.setDate(LocalDate.of(2026, 1, 2))
            runtime.authService.refreshToken(login.refreshToken)
            assertEquals(2, runtime.infrastructure.userRepository.activityWrites)
            assertEquals(OffsetDateTime.parse("2026-01-02T00:00:00Z"), user.lastLoginAt)
        }

    @Test
    fun `completed multi-factor sign-in records activity`() =
        runTest {
            lateinit var user: UserEntity
            val mfa =
                object : MfaService {
                    override fun decryptAndVerify(
                        secret: String,
                        code: String,
                    ): Boolean = true

                    override suspend fun generateNewChallenge(
                        userId: UUID,
                        userAgent: String?,
                        ip: InetAddress?,
                    ): UUID = UUID.randomUUID()

                    override suspend fun verifyChallenge(
                        challengeId: UUID,
                        code: String,
                        ip: InetAddress?,
                    ): UserEntity = user
                }
            val runtime = AuthScenarioRuntime(mfaService = mfa)
            user = runtime.insertUser(lastLoginAt = "2025-12-01T00:00:00Z", noticeStage = 7)

            runtime.authService.mfa(UUID.randomUUID(), "123456", "test", LOOPBACK)

            assertEquals(1, runtime.infrastructure.userRepository.activityWrites)
            assertNull(user.inactivityNoticeStage)
        }

    private suspend fun AuthScenarioRuntime.insertUser(
        lastLoginAt: String,
        noticeStage: Int? = null,
    ): UserEntity =
        UserEntity(
            email = "${UUID.randomUUID()}@example.com",
            passwordHash = PASSWORD,
            firstName = "Activity",
            lastName = "Test",
            lang = "en-US",
            defaultCurrency = "USD",
            tmz = "UTC",
            photoUrl = null,
            emailVerified = true,
            mfaEnabled = false,
            totpSecret = null,
            onboardingDone = true,
            lastLoginAt = OffsetDateTime.parse(lastLoginAt),
            inactivityNoticeStage = noticeStage,
        ).also {
            it.id = UUID.randomUUID()
            infrastructure.userRepository.insert(it).awaitSingle()
        }

    companion object {
        const val PASSWORD = "pass123"
        val LOOPBACK: InetAddress = InetAddress.getByName("127.0.0.1")
    }
}
