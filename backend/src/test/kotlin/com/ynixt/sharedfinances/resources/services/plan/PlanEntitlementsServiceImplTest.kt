package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.application.config.InactiveAccountDeletionProperties
import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanEntitlementsServiceImplTest {
    @Test
    fun `reports finite and unlimited quotas with a shared UTC reset instant`() =
        runTest {
            val userId = UUID.randomUUID()
            val service =
                PlanEntitlementsServiceImpl(
                    limitService = FakeLimits,
                    quotaService = FakeUsage,
                    clock = Clock.fixed(Instant.parse("2026-08-09T23:30:00Z"), ZoneOffset.UTC),
                    inactiveAccountDeletionProperties = InactiveAccountDeletionProperties(enabled = true),
                    planProperties = PlanProperties(enabled = true),
                )

            val entitlements = service.get(userId, UserPlanRole.USER, LAST_LOGIN_AT)
            assertEquals(UserPlanRole.USER, entitlements.role)
            assertTrue(entitlements.limitsEnabled)
            assertEquals(3, entitlements.importMaxLines)
            assertEquals(PlanLimitKey.entries.count { it.scope == PlanLimitScope.USER && it.countableQuota }, entitlements.quotas.size)
            assertEquals(3, entitlements.quotas.first { it.quota == PlanLimitKey.BANK_ACCOUNTS }.limit)
            assertEquals(2, entitlements.quotas.first { it.quota == PlanLimitKey.BANK_ACCOUNTS }.usage)
            assertNull(entitlements.quotas.first { it.quota == PlanLimitKey.BANK_ACCOUNTS }.windowEnd)
            assertEquals(
                Instant.parse("2026-09-01T00:00:00Z"),
                entitlements.quotas.first { it.quota == PlanLimitKey.IMPORTS_PER_MONTH }.windowEnd,
            )
            assertTrue(entitlements.quotas.none { it.quota == PlanLimitKey.INACTIVITY_RETENTION_MONTHS })
            assertTrue(entitlements.quotas.none { it.quota == PlanLimitKey.IMPORT_MAX_LINES })
            assertEquals(LAST_LOGIN_AT.plusMonths(3).toInstant(), entitlements.projectedDeletionAt)
        }

    @Test
    fun `administrator payload distinguishes unlimited from a numeric limit`() =
        runTest {
            val entitlements =
                PlanEntitlementsServiceImpl(
                    FakeLimits,
                    FakeUsage,
                    Clock.systemUTC(),
                    InactiveAccountDeletionProperties(enabled = true),
                    PlanProperties(enabled = true),
                ).get(UUID.randomUUID(), UserPlanRole.ADMINISTRATOR, LAST_LOGIN_AT)

            assertTrue(entitlements.quotas.all { it.unlimited && it.limit == null })
            assertNull(entitlements.importMaxLines)
        }

    @Test
    fun `pro payload resolves the pro plan`() =
        runTest {
            val entitlements =
                PlanEntitlementsServiceImpl(
                    FakeLimits,
                    FakeUsage,
                    Clock.systemUTC(),
                    InactiveAccountDeletionProperties(enabled = false),
                    PlanProperties(enabled = true),
                ).get(UUID.randomUUID(), UserPlanRole.PRO, LAST_LOGIN_AT)

            assertEquals(UserPlanRole.PRO, entitlements.role)
            assertTrue(entitlements.quotas.any { !it.unlimited && it.limit == 3 })
            assertNull(entitlements.projectedDeletionAt)
        }

    @Test
    fun `disabled plan model is reported explicitly with no import bound or deletion date`() =
        runTest {
            val entitlements =
                PlanEntitlementsServiceImpl(
                    FakeLimits,
                    FakeUsage,
                    Clock.systemUTC(),
                    InactiveAccountDeletionProperties(enabled = true),
                    PlanProperties(enabled = false),
                ).get(UUID.randomUUID(), UserPlanRole.USER, LAST_LOGIN_AT)

            assertTrue(!entitlements.limitsEnabled)
            assertNull(entitlements.projectedDeletionAt)
        }

    private object FakeLimits : PlanLimitService {
        override suspend fun resolve(
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ): ResolvedPlanLimit =
            if (plan == UserPlanRole.ADMINISTRATOR || quota == PlanLimitKey.OWNED_GROUPS) {
                ResolvedPlanLimit.unlimited()
            } else {
                ResolvedPlanLimit.finite(3)
            }

        override suspend fun save(limit: PlanLimitEntity): PlanLimitEntity = limit

        override suspend fun delete(
            scope: PlanLimitScope,
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = Unit
    }

    private object FakeUsage : PlanQuotaService {
        override suspend fun assertCanAdd(
            quotaOwnerUserId: UUID,
            quota: PlanLimitKey,
            requesterUserId: UUID,
        ) = Unit

        override suspend fun currentUsage(
            userId: UUID,
            quota: PlanLimitKey,
        ): Long = 2

        override suspend fun usageChanged(
            userId: UUID,
            quota: PlanLimitKey,
        ) = Unit
    }

    companion object {
        val LAST_LOGIN_AT: OffsetDateTime = OffsetDateTime.parse("2026-01-15T12:00:00Z")
    }
}
