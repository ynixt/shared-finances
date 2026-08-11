package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.PlanComparisonUnavailableException
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenPlanControllerTest {
    @Test
    fun `publishes enforced values without administrator and distinguishes unlimited limits`() =
        runTest {
            val limits = MutableLimits()
            limits.user(UserPlanRole.USER, PlanLimitKey.BANK_ACCOUNTS, 10)
            limits.user(UserPlanRole.PRO, PlanLimitKey.BANK_ACCOUNTS, 1000)
            limits.user(UserPlanRole.USER, PlanLimitKey.IMPORT_MAX_LINES, 100)
            limits.user(UserPlanRole.USER, PlanLimitKey.INACTIVITY_RETENTION_MONTHS, 12)
            limits.group(GroupPlanTier.COMMON, PlanLimitKey.GROUP_MEMBERS, 4)
            limits.group(GroupPlanTier.PRO, PlanLimitKey.GROUP_MEMBERS, 100)

            val payload = OpenPlanController(limits, PlanProperties(enabled = true)).comparison().body!!

            assertEquals(listOf(UserPlanRole.USER, UserPlanRole.PRO), payload.userPlans.map { it.plan })
            assertFalse(payload.userPlans.any { it.plan == UserPlanRole.ADMINISTRATOR })
            val commonUser = payload.userPlans.first { it.plan == UserPlanRole.USER }
            assertEquals(10, commonUser.limits.first { it.quota == PlanLimitKey.BANK_ACCOUNTS }.limit)
            assertEquals(100, commonUser.limits.first { it.quota == PlanLimitKey.IMPORT_MAX_LINES }.limit)
            assertFalse(commonUser.limits.any { it.quota == PlanLimitKey.INACTIVITY_RETENTION_MONTHS })
            assertEquals(12, commonUser.inactivityPolicy.retentionMonths)
            assertFalse(commonUser.inactivityPolicy.unlimited)

            val proBankAccounts =
                payload.userPlans
                    .first { it.plan == UserPlanRole.PRO }
                    .limits
                    .first { it.quota == PlanLimitKey.BANK_ACCOUNTS }
            assertEquals(1000, proBankAccounts.limit)
            assertFalse(proBankAccounts.unlimited)
            val proGoals =
                payload.userPlans
                    .first { it.plan == UserPlanRole.PRO }
                    .limits
                    .first { it.quota == PlanLimitKey.GOALS }
            assertNull(proGoals.limit)
            assertTrue(proGoals.unlimited)
            val commonGroupMembers =
                payload.groupTiers
                    .first { it.tier == GroupPlanTier.COMMON }
                    .limits
                    .first { it.quota == PlanLimitKey.GROUP_MEMBERS }
                    .limit
            assertEquals(4, commonGroupMembers)
            assertEquals(
                limits.resolve(UserPlanRole.USER, PlanLimitKey.BANK_ACCOUNTS).value,
                commonUser.limits.first { it.quota == PlanLimitKey.BANK_ACCOUNTS }.limit,
            )
            assertEquals(
                limits.resolve(UserPlanRole.PRO, PlanLimitKey.BANK_ACCOUNTS).value,
                proBankAccounts.limit,
            )
            assertEquals(
                limits.resolve(GroupPlanTier.COMMON, PlanLimitKey.GROUP_MEMBERS).value,
                commonGroupMembers,
            )
            assertEquals(
                limits.resolve(GroupPlanTier.PRO, PlanLimitKey.GROUP_MEMBERS).value,
                payload.groupTiers
                    .first { it.tier == GroupPlanTier.PRO }
                    .limits
                    .first { it.quota == PlanLimitKey.GROUP_MEMBERS }
                    .limit,
            )
        }

    @Test
    fun `reads changed stored values on a later request without a deployment`() =
        runTest {
            val limits = MutableLimits().apply { user(UserPlanRole.USER, PlanLimitKey.GOALS, 10) }
            val controller = OpenPlanController(limits, PlanProperties(enabled = true))

            val initial =
                controller
                    .comparison()
                    .body!!
                    .userPlans
                    .first()
                    .limits
                    .first { it.quota == PlanLimitKey.GOALS }
                    .limit
            assertEquals(10, initial)
            limits.user(UserPlanRole.USER, PlanLimitKey.GOALS, 12)
            val changed =
                controller
                    .comparison()
                    .body!!
                    .userPlans
                    .first()
                    .limits
                    .first { it.quota == PlanLimitKey.GOALS }
                    .limit
            assertEquals(12, changed)
        }

    @Test
    fun `is unavailable while plan enforcement is disabled`() =
        runTest {
            assertFailsWith<PlanComparisonUnavailableException> {
                OpenPlanController(MutableLimits(), PlanProperties(enabled = false)).comparison()
            }
        }

    private class MutableLimits : PlanLimitService {
        private val values = mutableMapOf<Triple<PlanLimitScope, String, PlanLimitKey>, Int?>()

        fun user(
            plan: UserPlanRole,
            quota: PlanLimitKey,
            value: Int?,
        ) {
            values[Triple(PlanLimitScope.USER, plan.name, quota)] = value
        }

        fun group(
            tier: GroupPlanTier,
            quota: PlanLimitKey,
            value: Int?,
        ) {
            values[Triple(PlanLimitScope.GROUP, tier.name, quota)] = value
        }

        override suspend fun resolve(
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = values[Triple(PlanLimitScope.USER, plan.name, quota)]?.let(ResolvedPlanLimit::finite) ?: ResolvedPlanLimit.unlimited()

        override suspend fun resolve(
            tier: GroupPlanTier,
            quota: PlanLimitKey,
        ) = values[Triple(PlanLimitScope.GROUP, tier.name, quota)]?.let(ResolvedPlanLimit::finite) ?: ResolvedPlanLimit.unlimited()

        override suspend fun save(limit: PlanLimitEntity): PlanLimitEntity = limit

        override suspend fun delete(
            scope: PlanLimitScope,
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = Unit
    }
}
