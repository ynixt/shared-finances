package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.application.config.LegalDocumentProperties
import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.UserActionEventService
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class UserPlanRoleServiceTest {
    @Test
    fun `registration uses the configured default while limits are off and changing it leaves existing users untouched`() =
        runTest {
            val repository = InMemoryUserRepository()
            val first = service(repository, UserPlanRole.USER).createUser(register("first@example.com"))
            val second = service(repository, UserPlanRole.PRO).createUser(register("second@example.com"))

            assertEquals(UserPlanRole.USER, repository.findById(first.id!!).block()!!.role)
            assertEquals(UserPlanRole.PRO, second.role)
        }

    private fun service(
        repository: InMemoryUserRepository,
        role: UserPlanRole,
    ): UserServiceImpl {
        val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
        Mockito.`when`(passwordEncoder.encode(Mockito.anyString())).thenReturn("hash")
        return UserServiceImpl(
            repository = repository,
            passwordEncoder = passwordEncoder,
            databaseHelperService = Mockito.mock(DatabaseHelperService::class.java),
            avatarService = Mockito.mock(AvatarService::class.java),
            legalDocumentProperties = LegalDocumentProperties(),
            authProperties = AuthProperties(),
            planProperties = PlanProperties(defaultRole = role, enabled = false),
            clock = Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC),
            accountDeletionService = Mockito.mock(AccountDeletionService::class.java),
            userActionEventService = Mockito.mock(UserActionEventService::class.java),
        )
    }

    private fun register(email: String) =
        RegisterDto(
            email = email,
            password = "secret1",
            firstName = "Plan",
            lastName = "User",
            lang = "en-US",
            defaultCurrency = "USD",
            tmz = "UTC",
            acceptTerms = true,
            acceptPrivacy = true,
        )
}
