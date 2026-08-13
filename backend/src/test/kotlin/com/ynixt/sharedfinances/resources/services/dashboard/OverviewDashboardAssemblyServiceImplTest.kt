package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.exceptions.http.ExchangeRateUnavailableException
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertFailsWith

class OverviewDashboardAssemblyServiceImplTest {
    private val exchangeRateService = Mockito.mock(ExchangeRateService::class.java)
    private val service = OverviewDashboardAssemblyServiceImpl(exchangeRateService)

    @Test
    fun `unresolved conversion raises exchange rate unavailable`() =
        runTest {
            val date = LocalDate.of(2026, 8, 11)
            Mockito.`when`(exchangeRateService.convertBatch(anyNonNull())).thenReturn(emptyMap())

            assertFailsWith<ExchangeRateUnavailableException> {
                service.convertRawValues(
                    rawValues =
                        listOf(
                            RawValue(
                                key = "balance",
                                value = BigDecimal("100"),
                                currency = "BRL",
                                referenceDate = date,
                            ),
                        ),
                    targetCurrency = "EUR",
                )
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = Mockito.any<T>() ?: null as T
}
