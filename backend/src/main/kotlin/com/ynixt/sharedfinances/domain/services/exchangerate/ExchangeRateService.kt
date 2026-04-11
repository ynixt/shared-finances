package com.ynixt.sharedfinances.domain.services.exchangerate

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import java.math.BigDecimal
import java.time.LocalDate

interface ExchangeRateService {
    suspend fun syncLatestQuotes(): Int

    suspend fun syncQuotesForDate(
        date: LocalDate,
        baseCurrencies: Set<String>? = null,
    ): Int

    suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity>

    suspend fun getRate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): BigDecimal

    suspend fun resolveRate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): ResolvedExchangeRate

    suspend fun convert(
        value: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): BigDecimal

    suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal>
}

data class ConversionRequest(
    val value: BigDecimal,
    val fromCurrency: String,
    val toCurrency: String,
    val referenceDate: LocalDate,
)

data class ResolvedExchangeRate(
    val rate: BigDecimal,
    /** Calendar date of the stored quote row used (nearest to [referenceDate] when exact match is missing). */
    val quoteDate: LocalDate,
)
