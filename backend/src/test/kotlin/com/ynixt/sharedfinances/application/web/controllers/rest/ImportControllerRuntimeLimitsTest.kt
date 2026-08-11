package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.imports.CreateImportDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportDuplicateCheckDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportDuplicateLineDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportLineDto
import com.ynixt.sharedfinances.application.web.validation.ImportLineLimitValidator
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.ImportLineLimitExceededException
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal
import com.ynixt.sharedfinances.domain.services.imports.ImportService
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ImportControllerRuntimeLimitsTest {
    @Test
    fun `accepts exact bound and rejects the next line with the bound that applied`() =
        runTest {
            val validator = ImportLineLimitValidator(FakeLimits(mapOf(UserPlanRole.USER to 2)))

            validator.validate(UserPlanRole.USER, 2)
            val exception = assertFailsWith<ImportLineLimitExceededException> { validator.validate(UserPlanRole.USER, 3) }

            assertEquals(2, exception.argsI18n?.get("maxLines"))
        }

    @Test
    fun `preferences and validation resolve each authenticated user independently without shared caching`() =
        runTest {
            val limits = FakeLimits(mapOf(UserPlanRole.USER to 2, UserPlanRole.PRO to 4))
            val validator = ImportLineLimitValidator(limits)
            val controller = ImportController(Mockito.mock(ImportService::class.java), validator)

            assertEquals(2, controller.preferences(principal(UserPlanRole.USER)).maxLines)
            assertEquals(4, controller.preferences(principal(UserPlanRole.PRO)).maxLines)
            assertFailsWith<ImportLineLimitExceededException> { validator.validate(UserPlanRole.USER, 3) }
            validator.validate(UserPlanRole.PRO, 3)

            assertEquals(listOf(UserPlanRole.USER, UserPlanRole.PRO, UserPlanRole.USER, UserPlanRole.PRO), limits.requests)
        }

    @Test
    fun `administrator and a disabled plan model are unbounded`() =
        runTest {
            val validator = ImportLineLimitValidator(FakeLimits(emptyMap()))

            validator.validate(UserPlanRole.ADMINISTRATOR, Int.MAX_VALUE)
            validator.validate(UserPlanRole.USER, Int.MAX_VALUE)
            assertNull(validator.maximum(UserPlanRole.ADMINISTRATOR))
            assertNull(validator.maximum(UserPlanRole.USER))
        }

    @Test
    fun `oversized duplicate check and import are rejected before service work`() =
        runTest {
            val importService = Mockito.mock(ImportService::class.java)
            val controller = ImportController(importService, ImportLineLimitValidator(FakeLimits(mapOf(UserPlanRole.USER to 2))))
            val principal = principal(UserPlanRole.USER)

            assertFailsWith<ImportLineLimitExceededException> {
                controller.checkDuplicates(principal, ImportDuplicateCheckDto(lines = List(3) { duplicateLine() }))
            }
            assertFailsWith<ImportLineLimitExceededException> {
                controller.create(
                    principal,
                    CreateImportDto(
                        fileHash = "a".repeat(64),
                        fileName = "oversized.csv",
                        lines = List(3) { importLine() },
                    ),
                )
            }
            Mockito.verifyNoInteractions(importService)
        }

    private class FakeLimits(
        private val values: Map<UserPlanRole, Int>,
    ) : PlanLimitService {
        val requests = mutableListOf<UserPlanRole>()

        override suspend fun resolve(
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ): ResolvedPlanLimit {
            require(quota == PlanLimitKey.IMPORT_MAX_LINES)
            requests += plan
            return values[plan]?.let(ResolvedPlanLimit::finite) ?: ResolvedPlanLimit.unlimited()
        }

        override suspend fun save(limit: PlanLimitEntity) = limit

        override suspend fun delete(
            scope: PlanLimitScope,
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = Unit
    }

    private fun principal(role: UserPlanRole): UserJwtAuthenticationToken =
        UserJwtAuthenticationToken(
            Mockito.mock(Jwt::class.java),
            UserPrincipal(
                id = UUID.randomUUID(),
                email = "${role.name.lowercase()}@example.com",
                firstName = "Import",
                lastName = "User",
                lang = "en-US",
                defaultCurrency = "USD",
                tmz = "UTC",
                photoUrl = null,
                emailVerified = true,
                mfaEnabled = false,
                onboardingDone = true,
                darkMode = false,
                role = role,
                lastLoginAt = OffsetDateTime.parse("2026-08-10T00:00:00Z"),
                authorities = emptyList(),
            ),
            emptyList(),
        )

    private fun duplicateLine() =
        ImportDuplicateLineDto(
            walletItemId = UUID.randomUUID(),
            name = "Line",
            value = BigDecimal.ONE,
            date = LocalDate.of(2026, 8, 8),
            installment = null,
        )

    private fun importLine() =
        ImportLineDto(
            walletItemId = UUID.randomUUID(),
            name = "Line",
            value = BigDecimal.ONE,
            date = LocalDate.of(2026, 8, 8),
            categoryId = null,
            groupId = null,
            beneficiaries = null,
            billDate = null,
            installment = null,
            installmentTotal = null,
            tags = null,
            observations = null,
        )
}
