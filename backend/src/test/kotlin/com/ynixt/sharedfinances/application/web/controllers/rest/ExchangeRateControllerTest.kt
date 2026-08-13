package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateResolveBatchRequestDto
import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateResolveRequestDto
import com.ynixt.sharedfinances.application.web.mapper.ExchangeRateQuoteDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDate

class ExchangeRateControllerTest {
    private val exchangeRateService = Mockito.mock(ExchangeRateService::class.java)
    private val mapper = Mockito.mock(ExchangeRateQuoteDtoMapper::class.java)
    private val principal = Mockito.mock(UserJwtAuthenticationToken::class.java)
    private val controller = ExchangeRateController(exchangeRateService, mapper)

    @Test
    fun `resolve returns rates and preserves unavailable combinations`() =
        runTest {
            val referenceDate = LocalDate.of(2026, 8, 7)
            val secondReferenceDate = referenceDate.plusDays(1)
            val quoteDate = LocalDate.of(2026, 8, 6)
            val available = ConversionRequest(BigDecimal.ONE, "USD", "BRL", referenceDate)
            val unavailable = ConversionRequest(BigDecimal.ONE, "EUR", "JPY", secondReferenceDate)
            Mockito.`when`(exchangeRateService.resolveRateBatch(listOf(available, unavailable))).thenReturn(
                mapOf(
                    available to ResolvedExchangeRate(rate = BigDecimal("5.25"), quoteDate = quoteDate),
                    unavailable to null,
                ),
            )

            val result =
                controller.resolve(
                    principal,
                    ExchangeRateResolveBatchRequestDto(
                        requests =
                            listOf(
                                ExchangeRateResolveRequestDto("usd", "brl", referenceDate),
                                ExchangeRateResolveRequestDto("eur", "jpy", secondReferenceDate),
                            ),
                    ),
                )

            assertEquals(BigDecimal("5.25"), result[0].rate)
            assertEquals(quoteDate, result[0].quoteDate)
            assertNull(result[1].rate)
            assertNull(result[1].quoteDate)
            Mockito.verify(exchangeRateService, Mockito.times(1)).resolveRateBatch(listOf(available, unavailable))
            Mockito.verify(exchangeRateService, Mockito.never()).resolveRate(
                Mockito.anyString(),
                Mockito.anyString(),
                anyNonNull(),
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = Mockito.any<T>() ?: null as T
}
