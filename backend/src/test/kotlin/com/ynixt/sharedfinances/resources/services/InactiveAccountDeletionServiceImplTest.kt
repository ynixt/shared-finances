package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.config.InactiveAccountDeletionProperties
import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
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
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InactiveAccountDeletionServiceImplTest {
    @Test
    fun `disabled switch performs no read warning or deletion`() =
        runTest {
            val fixture = Fixture(enabled = false, initialNow = Instant.parse("2030-01-01T00:00:00Z"))
            val user = fixture.addUser(lastLoginAt = "2020-01-01T00:00:00Z", noticeStage = 30)
            assertNull(fixture.service.runCleanup())
            assertEquals(0, fixture.users.activityQueries)
            assertEquals(30, user.inactivityNoticeStage)
            assertEquals(OffsetDateTime.parse("2020-01-01T00:00:00Z"), user.lastLoginAt)
            assertTrue(fixture.sender.sent.isEmpty())
            assertTrue(fixture.deletion.deleted.isEmpty())
        }

    @Test
    fun `enabled deletion switch performs no warning or deletion while plan model is disabled`() =
        runTest {
            val fixture = Fixture(planEnabled = false, initialNow = Instant.parse("2030-01-01T00:00:00Z"))
            fixture.addUser(lastLoginAt = "2020-01-01T00:00:00Z", noticeStage = 1)

            assertNull(fixture.service.runCleanup())
            assertEquals(0, fixture.users.activityQueries)
            assertTrue(fixture.sender.sent.isEmpty())
            assertTrue(fixture.deletion.deleted.isEmpty())
        }

    @Test
    fun `already dormant account is warned first and deleted only on a later run`() =
        runTest {
            val fixture = Fixture(initialNow = Instant.parse("2027-01-02T00:00:00Z"))
            val user = fixture.addUser(lastLoginAt = "2026-01-01T00:00:00Z")

            assertEquals(1, fixture.service.runCleanup()?.warningsSent)
            assertEquals(1, user.inactivityNoticeStage)
            assertTrue(fixture.deletion.deleted.isEmpty())

            assertEquals(1, fixture.service.runCleanup()?.accountsDeleted)
            assertEquals(listOf(user.id), fixture.deletion.deleted)
        }

    @Test
    fun `warning ladder advances once per due stage and sign-in cancels it`() =
        runTest {
            val fixture = Fixture(initialNow = Instant.parse("2026-12-02T00:00:00Z"))
            val user = fixture.addUser(lastLoginAt = "2026-01-01T00:00:00Z")

            fixture.service.runCleanup()
            assertEquals(30, user.inactivityNoticeStage)
            fixture.service.runCleanup()
            assertEquals(1, fixture.sender.sent.size)

            fixture.clock.now = Instant.parse("2026-12-25T00:00:00Z")
            fixture.service.runCleanup()
            assertEquals(7, user.inactivityNoticeStage)

            user.lastLoginAt = OffsetDateTime.parse("2026-12-25T01:00:00Z")
            user.inactivityNoticeStage = null
            fixture.clock.now = Instant.parse("2026-12-31T00:00:00Z")
            fixture.service.runCleanup()

            assertNull(user.inactivityNoticeStage)
            assertEquals(2, fixture.sender.sent.size)
            assertTrue(fixture.deletion.deleted.isEmpty())
        }

    @Test
    fun `full ladder reaches deletion only after the last warning`() =
        runTest {
            val fixture = Fixture(initialNow = Instant.parse("2026-12-02T00:00:00Z"))
            val user = fixture.addUser(lastLoginAt = "2026-01-01T00:00:00Z")

            fixture.service.runCleanup()
            fixture.clock.now = Instant.parse("2026-12-25T00:00:00Z")
            fixture.service.runCleanup()
            fixture.clock.now = Instant.parse("2026-12-31T00:00:00Z")
            fixture.service.runCleanup()
            assertEquals(listOf(30, 7, 1), fixture.sender.sent.map { message -> stageFrom(message.subject) })

            fixture.clock.now = Instant.parse("2027-01-01T00:00:00Z")
            fixture.service.runCleanup()
            assertEquals(listOf(user.id), fixture.deletion.deleted)
        }

    @Test
    fun `failed mail is not recorded and is retried`() =
        runTest {
            val fixture = Fixture(initialNow = Instant.parse("2027-01-02T00:00:00Z"))
            val user = fixture.addUser(lastLoginAt = "2026-01-01T00:00:00Z")
            fixture.sender.failNext = true

            assertEquals(0, fixture.service.runCleanup()?.warningsSent)
            assertNull(user.inactivityNoticeStage)
            assertEquals(1, fixture.sender.attempts)

            assertEquals(1, fixture.service.runCleanup()?.warningsSent)
            assertEquals(1, user.inactivityNoticeStage)
            assertEquals(2, fixture.sender.attempts)
        }

    @Test
    fun `unlimited and administrator accounts are never warned or deleted`() =
        runTest {
            val fixture = Fixture(initialNow = Instant.parse("2030-01-01T00:00:00Z"))
            fixture.addUser(role = UserPlanRole.PRO, lastLoginAt = "2020-01-01T00:00:00Z")
            fixture.addUser(role = UserPlanRole.ADMINISTRATOR, lastLoginAt = "2020-01-01T00:00:00Z")

            assertEquals(0, fixture.service.runCleanup()?.warningsSent)
            assertTrue(fixture.sender.sent.isEmpty())
            assertTrue(fixture.deletion.deleted.isEmpty())
        }

    @Test
    fun `one failed deletion does not stop the next account`() =
        runTest {
            val fixture = Fixture(initialNow = Instant.parse("2027-01-02T00:00:00Z"))
            val first = fixture.addUser(lastLoginAt = "2026-01-01T00:00:00Z", noticeStage = 1)
            val second = fixture.addUser(lastLoginAt = "2026-01-01T00:00:00Z", noticeStage = 1)
            fixture.deletion.failIds += requireNotNull(first.id)

            assertEquals(1, fixture.service.runCleanup()?.accountsDeleted)
            assertEquals(listOf(second.id), fixture.deletion.deleted)
        }

    @Test
    fun `warning lists owned groups and not mere memberships`() =
        runTest {
            val fixture = Fixture(initialNow = Instant.parse("2027-01-02T00:00:00Z"))
            val user = fixture.addUser(lastLoginAt = "2026-01-01T00:00:00Z")
            fixture.groups.save(GroupEntity("Owned budget", user.id!!)).awaitSingle()

            fixture.service.runCleanup()

            val text =
                fixture.sender.sent
                    .single()
                    .textBody
            assertTrue(text.contains("Owned budget"))
            assertTrue(!text.contains("Member only"))
        }

    private class Fixture(
        enabled: Boolean = true,
        planEnabled: Boolean = true,
        initialNow: Instant,
    ) {
        val users = InMemoryUserRepository()
        val groups = InMemoryAccountDeletionGroupStore()
        val sender = RecordingSender()
        val deletion = RecordingDeletion()
        val clock = MutableClock(initialNow)
        val service: InactiveAccountDeletionServiceImpl

        init {
            val messageSource =
                ResourceBundleMessageSource().apply {
                    setBasename("i18n/mail/messages")
                    setDefaultEncoding("UTF-8")
                    setFallbackToSystemLocale(false)
                }
            service =
                InactiveAccountDeletionServiceImpl(
                    properties = InactiveAccountDeletionProperties(enabled = enabled),
                    planProperties = PlanProperties(enabled = planEnabled),
                    userRepository = users,
                    groupRepository = groups,
                    planLimitService = RetentionLimits,
                    composer = AccountLifecycleMailMessageComposer(messageSource),
                    dispatchService = sender,
                    accountDeletionService = deletion,
                    clock = clock,
                )
        }

        suspend fun addUser(
            role: UserPlanRole = UserPlanRole.USER,
            lastLoginAt: String,
            noticeStage: Int? = null,
        ): UserEntity =
            UserEntity(
                email = "${UUID.randomUUID()}@example.com",
                passwordHash = "hash",
                firstName = "Inactive",
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
                lastLoginAt = OffsetDateTime.parse(lastLoginAt),
                inactivityNoticeStage = noticeStage,
            ).also {
                it.id = UUID.randomUUID()
                users.insert(it).awaitSingle()
            }
    }

    private object RetentionLimits : PlanLimitService {
        override suspend fun resolve(
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ): ResolvedPlanLimit =
            if (plan == UserPlanRole.USER && quota == PlanLimitKey.INACTIVITY_RETENTION_MONTHS) {
                ResolvedPlanLimit.finite(12)
            } else {
                ResolvedPlanLimit.unlimited()
            }

        override suspend fun save(limit: PlanLimitEntity): PlanLimitEntity = limit

        override suspend fun delete(
            scope: PlanLimitScope,
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = Unit
    }

    private class RecordingSender : TransactionalEmailSender {
        val sent = mutableListOf<TransactionalEmailMessage>()
        var attempts = 0
        var failNext = false

        override suspend fun send(message: TransactionalEmailMessage) {
            attempts++
            if (failNext) {
                failNext = false
                error("mail unavailable")
            }
            sent += message
        }
    }

    private class RecordingDeletion : AccountDeletionService {
        val deleted = mutableListOf<UUID?>()
        val failIds = mutableSetOf<UUID>()

        override suspend fun deleteAccountForUser(userId: UUID) {
            if (userId in failIds) error("deletion failed")
            deleted += userId
        }
    }

    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

    companion object {
        private fun stageFrom(subject: String): Int =
            when {
                subject.startsWith("Final") -> 1
                subject.contains("soon") -> 7
                else -> 30
            }
    }
}
