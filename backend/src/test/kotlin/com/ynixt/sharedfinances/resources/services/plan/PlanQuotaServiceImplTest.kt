package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.PlanQuotaExceededException
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.repositories.PlanQuotaUsageRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.plan.GroupPlanTierService
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class PlanQuotaServiceImplTest {
    @Test
    fun `group holdings allow below the limit and refuse at it without consulting personal usage`() =
        runTest {
            val groupId = UUID.randomUUID()
            val usage = GroupUsage(0)
            val limits = Mockito.mock(PlanLimitService::class.java)
            val quotas =
                listOf(
                    PlanLimitKey.GROUP_CATEGORIES,
                    PlanLimitKey.GROUP_GOALS,
                    PlanLimitKey.GROUP_ACTIVE_SCHEDULES,
                )
            quotas.forEach {
                Mockito.`when`(limits.resolve(GroupPlanTier.COMMON, it)).thenReturn(ResolvedPlanLimit.finite(1))
            }
            val tiers = Mockito.mock(GroupPlanTierService::class.java)
            Mockito.`when`(tiers.resolve(groupId)).thenReturn(GroupPlanTier.COMMON)
            val users = Mockito.mock(UserRepository::class.java)
            val service =
                PlanQuotaServiceImpl(
                    userRepository = users,
                    limitService = limits,
                    usageRepository = usage,
                    clock = Clock.systemUTC(),
                    actionEventService = Mockito.mock(com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService::class.java),
                    groupPlanTierService = tiers,
                )

            quotas.forEach { quota ->
                usage.value = 0
                service.assertGroupCanAdd(groupId, quota, USER_ID)
                usage.value = 1
                val error =
                    assertThrows<PlanQuotaExceededException> {
                        service.assertGroupCanAdd(groupId, quota, USER_ID)
                    }
                assertEquals(groupId, error.groupId)
                assertEquals(quota, error.quota)
            }
            assertEquals(quotas.size * 2, usage.locks)
            Mockito.verifyNoInteractions(users)
        }

    @Test
    fun `common and pro membership bounds admit four and one hundred people including the owner`() =
        runTest {
            val commonGroup = UUID.randomUUID()
            val proGroup = UUID.randomUUID()
            val usage = GroupUsage(3)
            val limits = Mockito.mock(PlanLimitService::class.java)
            Mockito.`when`(limits.resolve(GroupPlanTier.COMMON, PlanLimitKey.GROUP_MEMBERS)).thenReturn(ResolvedPlanLimit.finite(4))
            Mockito.`when`(limits.resolve(GroupPlanTier.PRO, PlanLimitKey.GROUP_MEMBERS)).thenReturn(ResolvedPlanLimit.finite(100))
            val tiers = Mockito.mock(GroupPlanTierService::class.java)
            Mockito.`when`(tiers.resolve(commonGroup)).thenReturn(GroupPlanTier.COMMON)
            Mockito.`when`(tiers.resolve(proGroup)).thenReturn(GroupPlanTier.PRO)
            val service =
                PlanQuotaServiceImpl(
                    userRepository = Mockito.mock(UserRepository::class.java),
                    limitService = limits,
                    usageRepository = usage,
                    clock = Clock.systemUTC(),
                    actionEventService = Mockito.mock(com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService::class.java),
                    groupPlanTierService = tiers,
                )

            service.assertGroupCanAdd(commonGroup, PlanLimitKey.GROUP_MEMBERS, USER_ID)
            usage.value = 4
            assertThrows<PlanQuotaExceededException> {
                service.assertGroupCanAdd(commonGroup, PlanLimitKey.GROUP_MEMBERS, USER_ID)
            }

            usage.value = 99
            service.assertGroupCanAdd(proGroup, PlanLimitKey.GROUP_MEMBERS, USER_ID)
            usage.value = 100
            assertThrows<PlanQuotaExceededException> {
                service.assertGroupCanAdd(proGroup, PlanLimitKey.GROUP_MEMBERS, USER_ID)
            }
        }

    @Test
    fun `allows below the limit and refuses at it`() =
        runTest {
            val usage = FakeUsage(2)
            val service = service(UserPlanRole.USER, ResolvedPlanLimit.finite(3), usage)
            service.assertCanAdd(USER_ID, PlanLimitKey.GOALS)
            assertEquals(1, usage.locks)

            usage.value = 3
            assertThrows<PlanQuotaExceededException> {
                service.assertCanAdd(USER_ID, PlanLimitKey.GOALS)
            }
        }

    @Test
    fun `administrator and unlimited quota always allow`() =
        runTest {
            val adminUsage = FakeUsage(Long.MAX_VALUE)
            service(UserPlanRole.ADMINISTRATOR, ResolvedPlanLimit.finite(0), adminUsage)
                .assertCanAdd(USER_ID, PlanLimitKey.GOALS)
            assertEquals(0, adminUsage.locks)

            val unlimitedUsage = FakeUsage(Long.MAX_VALUE)
            service(UserPlanRole.USER, ResolvedPlanLimit.unlimited(), unlimitedUsage)
                .assertCanAdd(USER_ID, PlanLimitKey.GOALS)
            assertEquals(1, unlimitedUsage.locks)
        }

    @Test
    fun `unlimited resolution never refuses any enforcement point even when usage is beyond every stored bound`() =
        runTest {
            val personalUsage = FakeUsage(Long.MAX_VALUE)
            val personal = service(UserPlanRole.USER, ResolvedPlanLimit.unlimited(), personalUsage)
            PlanLimitKey.entries
                .filter {
                    it.scope == com.ynixt.sharedfinances.domain.enums.PlanLimitScope.USER && it.countableQuota
                }.forEach {
                    personal.assertCanAdd(USER_ID, it)
                }

            val groupId = UUID.randomUUID()
            val groupUsage = GroupUsage(Long.MAX_VALUE)
            val limits = Mockito.mock(PlanLimitService::class.java)
            val tiers = Mockito.mock(GroupPlanTierService::class.java)
            Mockito.`when`(tiers.resolve(groupId)).thenReturn(GroupPlanTier.COMMON)
            PlanLimitKey.entries
                .filter { it.scope == com.ynixt.sharedfinances.domain.enums.PlanLimitScope.GROUP }
                .forEach {
                    Mockito.`when`(limits.resolve(GroupPlanTier.COMMON, it)).thenReturn(ResolvedPlanLimit.unlimited())
                }
            val group =
                PlanQuotaServiceImpl(
                    userRepository = Mockito.mock(UserRepository::class.java),
                    limitService = limits,
                    usageRepository = groupUsage,
                    clock = Clock.systemUTC(),
                    actionEventService = Mockito.mock(com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService::class.java),
                    groupPlanTierService = tiers,
                )
            PlanLimitKey.entries
                .filter { it.scope == com.ynixt.sharedfinances.domain.enums.PlanLimitScope.GROUP }
                .forEach {
                    group.assertGroupCanAdd(groupId, it, USER_ID)
                }
        }

    @Test
    fun `pro and administrator accounts remain usable across every quota`() =
        runTest {
            PlanLimitKey.entries.filter { it.scope == com.ynixt.sharedfinances.domain.enums.PlanLimitScope.USER }.forEach { quota ->
                val proUsage = FakeUsage(if (quota == PlanLimitKey.OWNED_GROUPS) 99 else 999)
                service(
                    UserPlanRole.PRO,
                    ResolvedPlanLimit.finite(if (quota == PlanLimitKey.OWNED_GROUPS) 100 else 1000),
                    proUsage,
                ).assertCanAdd(USER_ID, quota)

                val administratorUsage = FakeUsage(Long.MAX_VALUE)
                service(UserPlanRole.ADMINISTRATOR, ResolvedPlanLimit.finite(0), administratorUsage)
                    .assertCanAdd(USER_ID, quota)
                assertEquals(0, administratorUsage.locks)
            }
        }

    @Test
    fun `refusal identifies a different quota owner`() =
        runTest {
            val owner = UUID.randomUUID()
            val requester = UUID.randomUUID()
            val error =
                assertThrows<PlanQuotaExceededException> {
                    service(UserPlanRole.USER, ResolvedPlanLimit.finite(1), FakeUsage(1), owner)
                        .assertCanAdd(owner, PlanLimitKey.OWNED_GROUPS, requester)
                }
            assertEquals(owner, error.quotaOwnerUserId)
        }

    @Test
    fun `monthly window resets for every user at the same UTC instant`() =
        runTest {
            val clock = MutableClock(Instant.parse("2026-08-31T23:59:59Z"))
            val usage = RecordingMonthlyUsage()
            val service =
                PlanQuotaServiceImpl(
                    userRepository = Mockito.mock(UserRepository::class.java),
                    limitService = Mockito.mock(PlanLimitService::class.java),
                    usageRepository = usage,
                    clock = clock,
                    actionEventService = Mockito.mock(com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService::class.java),
                )

            service.currentUsage(UUID.randomUUID(), PlanLimitKey.IMPORTS_PER_MONTH)
            service.currentUsage(UUID.randomUUID(), PlanLimitKey.IMPORTS_PER_MONTH)
            assertEquals(listOf(Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z")), usage.windows)

            clock.instant = clock.instant.plus(Duration.ofSeconds(1))
            service.currentUsage(UUID.randomUUID(), PlanLimitKey.IMPORTS_PER_MONTH)
            assertEquals(Instant.parse("2026-09-01T00:00:00Z"), usage.windows.last())
        }

    @Test
    fun `stock quotas enforce independently and deletion frees capacity`() =
        runTest {
            val stockQuotas =
                listOf(
                    PlanLimitKey.BANK_ACCOUNTS,
                    PlanLimitKey.CREDIT_CARDS,
                    PlanLimitKey.CATEGORIES,
                    PlanLimitKey.GOALS,
                    PlanLimitKey.ACTIVE_SCHEDULES,
                )
            val usage = KeyedUsage(stockQuotas.associateWith { 1L }.toMutableMap())
            val service = service(UserPlanRole.USER, ResolvedPlanLimit.finite(1), usage)

            stockQuotas.forEachIndexed { index, quota ->
                usage.values[quota] = 0
                service.assertCanAdd(USER_ID, quota)

                usage.values[quota] = 1
                assertThrows<PlanQuotaExceededException> { service.assertCanAdd(USER_ID, quota) }

                usage.values[quota] = 0
                service.assertCanAdd(USER_ID, quota)

                val independentQuota = stockQuotas[(index + 1) % stockQuotas.size]
                assertThrows<PlanQuotaExceededException> { service.assertCanAdd(USER_ID, independentQuota) }
                usage.values[quota] = 1
            }
        }

    private fun service(
        role: UserPlanRole,
        limit: ResolvedPlanLimit,
        usage: PlanQuotaUsageRepository,
        userId: UUID = USER_ID,
    ): PlanQuotaServiceImpl {
        val users = Mockito.mock(UserRepository::class.java)
        Mockito.`when`(users.findById(userId)).thenReturn(Mono.just(user(userId, role)))
        val limits = Mockito.mock(PlanLimitService::class.java)
        PlanLimitKey.entries.forEach { quota ->
            runTest { Mockito.`when`(limits.resolve(role, quota)).thenReturn(limit) }
        }
        return PlanQuotaServiceImpl(
            userRepository = users,
            limitService = limits,
            usageRepository = usage,
            clock = Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC),
            actionEventService = Mockito.mock(com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService::class.java),
        )
    }

    private fun user(
        id: UUID,
        role: UserPlanRole,
    ) = UserEntity(
        email = "quota@example.com",
        passwordHash = null,
        firstName = "Quota",
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

    private class FakeUsage(
        var value: Long,
    ) : PlanQuotaUsageRepository {
        var locks = 0

        override suspend fun acquireTransactionLock(
            userId: UUID,
            quota: PlanLimitKey,
        ) {
            locks++
        }

        override suspend fun countUsage(
            userId: UUID,
            quota: PlanLimitKey,
            utcMonthStart: Instant,
        ): Long = value
    }

    private class GroupUsage(
        var value: Long,
    ) : PlanQuotaUsageRepository {
        var locks = 0

        override suspend fun acquireTransactionLock(
            userId: UUID,
            quota: PlanLimitKey,
        ) = Unit

        override suspend fun countUsage(
            userId: UUID,
            quota: PlanLimitKey,
            utcMonthStart: Instant,
        ) = 0L

        override suspend fun acquireGroupTransactionLock(
            groupId: UUID,
            quota: PlanLimitKey,
        ) {
            locks++
        }

        override suspend fun countGroupUsage(
            groupId: UUID,
            quota: PlanLimitKey,
            includeOutstandingInvitations: Boolean,
        ) = value
    }

    private class RecordingMonthlyUsage : PlanQuotaUsageRepository {
        val windows = mutableListOf<Instant>()

        override suspend fun acquireTransactionLock(
            userId: UUID,
            quota: PlanLimitKey,
        ) = Unit

        override suspend fun countUsage(
            userId: UUID,
            quota: PlanLimitKey,
            utcMonthStart: Instant,
        ): Long {
            windows += utcMonthStart
            return 0
        }
    }

    private class KeyedUsage(
        val values: MutableMap<PlanLimitKey, Long>,
    ) : PlanQuotaUsageRepository {
        override suspend fun acquireTransactionLock(
            userId: UUID,
            quota: PlanLimitKey,
        ) = Unit

        override suspend fun countUsage(
            userId: UUID,
            quota: PlanLimitKey,
            utcMonthStart: Instant,
        ): Long = values.getValue(quota)
    }

    private class MutableClock(
        var instant: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = instant
    }

    companion object {
        private val USER_ID = UUID.randomUUID()
    }
}
