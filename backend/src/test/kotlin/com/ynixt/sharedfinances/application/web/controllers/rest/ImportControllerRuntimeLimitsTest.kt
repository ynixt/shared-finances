package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.config.ImportProperties
import com.ynixt.sharedfinances.application.web.dto.imports.CreateImportDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportDuplicateCheckDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportDuplicateLineDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportLineDto
import com.ynixt.sharedfinances.application.web.validation.ImportLineLimitValidator
import com.ynixt.sharedfinances.domain.exceptions.http.ImportLineLimitExceededException
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.imports.ImportService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImportControllerRuntimeLimitsTest {
    private val importService = Mockito.mock(ImportService::class.java)
    private val properties = ImportProperties(maxLines = 2)
    private val validator = ImportLineLimitValidator(properties)
    private val controller = ImportController(importService, properties, validator)
    private val principal = Mockito.mock(UserJwtAuthenticationToken::class.java)

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(importService, principal)
    }

    @Test
    fun `reports configured maximum and accepts the exact boundary`() =
        runTest {
            assertEquals(2, controller.preferences().maxLines)
            validator.validate(2)
        }

    @Test
    fun `rejects duplicate checks over the limit before service work`() =
        runTest {
            val request = ImportDuplicateCheckDto(lines = List(3) { duplicateLine() })

            assertFailsWith<ImportLineLimitExceededException> { controller.checkDuplicates(principal, request) }
            Mockito.verifyNoInteractions(importService)
        }

    @Test
    fun `rejects import creation over the limit before persistence or dispatch`() =
        runTest {
            val request =
                CreateImportDto(
                    fileHash = "a".repeat(64),
                    fileName = "oversized.csv",
                    lines = List(3) { importLine() },
                )

            val exception = assertFailsWith<ImportLineLimitExceededException> { controller.create(principal, request) }
            assertEquals(2, exception.argsI18n?.get("maxLines"))
            Mockito.verifyNoInteractions(importService)
        }

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
