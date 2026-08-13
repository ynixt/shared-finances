package com.ynixt.sharedfinances.domain.services.exchangerate

import java.math.BigDecimal
import java.time.LocalDate

interface ExchangeRateProvider {
    val source: String

    suspend fun fetchUsdRates(date: LocalDate? = null): List<Quote>

    data class Quote(
        val currency: String,
        val quoteDate: LocalDate,
        val rate: BigDecimal,
    )
}
