package com.ynixt.sharedfinances.domain.services.exchangerate

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

interface ExchangeRateProvider {
    val source: String

    suspend fun fetchLatest(
        baseCurrency: String,
        quoteCurrencies: Set<String>,
    ): List<Quote>

    suspend fun fetchForDate(
        baseCurrency: String,
        quoteCurrencies: Set<String>,
        date: LocalDate,
    ): List<Quote>

    data class Quote(
        val baseCurrency: String,
        val quoteCurrency: String,
        val quoteDate: LocalDate,
        val quotedAt: OffsetDateTime,
        val rate: BigDecimal,
    )
}
