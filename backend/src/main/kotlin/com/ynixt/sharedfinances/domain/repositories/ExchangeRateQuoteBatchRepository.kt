package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface ExchangeRateQuoteBatchRepository {
    fun upsertDailyBatch(quotes: Collection<ExchangeRateQuoteUpsert>): Mono<Long>

    fun findClosestOnOrBeforeDateForCurrencies(
        currencies: Set<String>,
        referenceDate: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>

    fun findClosestOnOrAfterDateForCurrencies(
        currencies: Set<String>,
        referenceDate: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>

    fun findAllByCurrenciesAndQuoteDateBetween(
        currencies: Set<String>,
        quoteDateFrom: LocalDate,
        quoteDateTo: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>
}

data class ExchangeRateQuoteUpsert(
    val id: UUID,
    val source: String,
    val currency: String,
    val quoteDate: LocalDate,
    val rate: BigDecimal,
    val fetchedAt: OffsetDateTime,
)
