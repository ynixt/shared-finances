package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.application.config.LegalDocumentProperties
import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.scenarios.support.NoOpAvatarService
import com.ynixt.sharedfinances.scenarios.support.NoOpDatabaseHelperService
import com.ynixt.sharedfinances.scenarios.support.NoOpUserActionEventService
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryUserRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserLegalDocumentRegistrationTest {
    private val clock = Clock.fixed(Instant.parse("2026-08-10T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `registration without presented documents stores no acceptance and remains usable after enablement`() =
        runTest {
            val repository = InMemoryUserRepository()
            val created = service(repository, enabled = false).createUser(register(acceptance = false))

            assertNull(created.termsAcceptedAt)
            assertNull(created.termsVersion)
            assertNull(created.privacyAcceptedAt)
            assertNull(created.privacyVersion)

            service(repository, enabled = true).changeLanguage(created.id!!, "pt-BR")
            val usable = repository.findById(created.id!!).awaitSingle()
            assertEquals("pt-BR", usable.lang)
            assertNull(usable.termsVersion)
            assertNull(usable.privacyVersion)
        }

    @Test
    fun `registration with presented documents stores both current versions at the same instant`() =
        runTest {
            val created = service(InMemoryUserRepository(), enabled = true).createUser(register(acceptance = true))

            assertNotNull(created.termsAcceptedAt)
            assertEquals(created.termsAcceptedAt, created.privacyAcceptedAt)
            assertEquals("terms-v2", created.termsVersion)
            assertEquals("privacy-v2", created.privacyVersion)
        }

    private fun service(
        repository: InMemoryUserRepository,
        enabled: Boolean,
    ): UserServiceImpl {
        val encoder = Mockito.mock(PasswordEncoder::class.java)
        Mockito.`when`(encoder.encode(Mockito.anyString())).thenReturn("hash")
        return UserServiceImpl(
            repository = repository,
            passwordEncoder = encoder,
            databaseHelperService = NoOpDatabaseHelperService(),
            avatarService = NoOpAvatarService(),
            legalDocumentProperties =
                LegalDocumentProperties(
                    enabled = enabled,
                    termsVersion = "terms-v2",
                    privacyVersion = "privacy-v2",
                ),
            authProperties = AuthProperties(),
            planProperties = PlanProperties(),
            clock = clock,
            accountDeletionService = Mockito.mock(AccountDeletionService::class.java),
            userActionEventService = NoOpUserActionEventService(),
        )
    }

    private fun register(acceptance: Boolean) =
        RegisterDto(
            email = "legal-$acceptance@example.com",
            password = "secret1",
            firstName = "Legal",
            lastName = "User",
            lang = "en-US",
            defaultCurrency = "USD",
            tmz = "UTC",
            acceptTerms = acceptance,
            acceptPrivacy = acceptance,
        )
}
