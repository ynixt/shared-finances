package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.config.AuthFeatureFlags
import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.application.config.AuthTokenTtl
import com.ynixt.sharedfinances.application.config.InactiveAccountDeletionProperties
import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.mail.TransactionalEmailMessage
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.domain.services.mail.TransactionalEmailSender
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.resources.services.mail.AccountLifecycleMailMessageComposer
import com.ynixt.sharedfinances.scenarios.accountdeletion.support.InMemoryAccountDeletionGroupStore
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryUserRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.context.support.ResourceBundleMessageSource
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class AccountCleanupInteractionTest {
    @Test
    fun `account eligible for both cleanups is deleted once and then treated as gone`() =
        runTest {
            val users = InMemoryUserRepository()
            val user =
                UserEntity(
                    email = "dual-${UUID.randomUUID()}@example.com",
                    passwordHash = "hash",
                    firstName = "Never",
                    lastName = "Confirmed",
                    lang = "en-US",
                    defaultCurrency = "USD",
                    tmz = "UTC",
                    photoUrl = null,
                    emailVerified = false,
                    mfaEnabled = false,
                    totpSecret = null,
                    onboardingDone = false,
                    role = UserPlanRole.USER,
                    lastLoginAt = OffsetDateTime.parse("2020-01-01T00:00:00Z"),
                    inactivityNoticeStage = 1,
                ).also {
                    it.id = UUID.randomUUID()
                    it.createdAt = OffsetDateTime.parse("2020-01-01T00:00:00Z")
                    users.insert(it).awaitSingle()
                }
            val deletion = RemovingDeletion(users)
            val clock = Clock.fixed(Instant.parse("2027-01-01T00:00:00Z"), ZoneOffset.UTC)

            UnconfirmedAccountCleanupServiceImpl(
                authProperties =
                    AuthProperties(
                        features = AuthFeatureFlags(emailConfirmationEnabled = true),
                        emailConfirmation = AuthTokenTtl(ttlMinutes = 60),
                    ),
                userRepository = users,
                accountDeletionService = deletion,
                clock = clock,
            ).runCleanup()

            inactiveService(users, deletion, clock).runCleanup()

            assertEquals(listOf(user.id), deletion.deleted)
        }

    private fun inactiveService(
        users: InMemoryUserRepository,
        deletion: AccountDeletionService,
        clock: Clock,
    ): InactiveAccountDeletionServiceImpl {
        val source =
            ResourceBundleMessageSource().apply {
                setBasename("i18n/mail/messages")
                setDefaultEncoding("UTF-8")
                setFallbackToSystemLocale(false)
            }
        return InactiveAccountDeletionServiceImpl(
            properties = InactiveAccountDeletionProperties(enabled = true),
            planProperties = PlanProperties(enabled = true),
            userRepository = users,
            groupRepository = InMemoryAccountDeletionGroupStore(),
            planLimitService = RetentionLimits,
            composer = AccountLifecycleMailMessageComposer(source),
            dispatchService = NoOpSender,
            accountDeletionService = deletion,
            clock = clock,
        )
    }

    private class RemovingDeletion(
        private val users: InMemoryUserRepository,
    ) : AccountDeletionService {
        val deleted = mutableListOf<UUID?>()

        override suspend fun deleteAccountForUser(userId: UUID) {
            if (users.existsById(userId).awaitSingle()) {
                users.deleteById(userId).awaitSingle()
                deleted += userId
            }
        }
    }

    private object NoOpSender : TransactionalEmailSender {
        override suspend fun send(message: TransactionalEmailMessage) = Unit
    }

    private object RetentionLimits : PlanLimitService {
        override suspend fun resolve(
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ): ResolvedPlanLimit = ResolvedPlanLimit.finite(12)

        override suspend fun save(limit: PlanLimitEntity): PlanLimitEntity = limit

        override suspend fun delete(
            scope: PlanLimitScope,
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = Unit
    }
}
