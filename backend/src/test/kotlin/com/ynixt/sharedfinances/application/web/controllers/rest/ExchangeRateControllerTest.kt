package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateResolveBatchRequestDto
import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateResolveRequestDto
import com.ynixt.sharedfinances.application.web.mapper.ExchangeRateQuoteDtoMapper
import com.ynixt.sharedfinances.domain.exceptions.http.ExchangeRateUnavailableException
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
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
            val quoteDate = LocalDate.of(2026, 8, 6)
            Mockito
                .`when`(exchangeRateService.resolveRate("USD", "BRL", referenceDate))
                .thenReturn(ResolvedExchangeRate(rate = BigDecimal("5.25"), quoteDate = quoteDate))
            Mockito
                .`when`(exchangeRateService.resolveRate("EUR", "BRL", referenceDate))
                .thenThrow(ExchangeRateUnavailableException("EUR", "BRL", referenceDate))

            val result =
                controller.resolve(
                    principal,
                    ExchangeRateResolveBatchRequestDto(
                        requests =
                            listOf(
                                ExchangeRateResolveRequestDto("usd", "brl", referenceDate),
                                ExchangeRateResolveRequestDto("eur", "brl", referenceDate),
                            ),
                    ),
                )

            assertEquals(BigDecimal("5.25"), result[0].rate)
            assertEquals(quoteDate, result[0].quoteDate)
            assertNull(result[1].rate)
            assertNull(result[1].quoteDate)
        }
}
