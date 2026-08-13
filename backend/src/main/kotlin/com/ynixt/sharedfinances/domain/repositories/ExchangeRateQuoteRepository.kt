package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuotePairRates
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface ExchangeRateQuoteRepository : EntityRepository<ExchangeRateQuoteEntity> {
    fun upsertDaily(
        id: UUID,
        source: String,
        currency: String,
        quoteDate: LocalDate,
        rate: BigDecimal,
        fetchedAt: OffsetDateTime,
    ): Mono<Long>

    fun findClosestOnOrBeforeDate(
        currency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuoteEntity>

    fun findClosestOnOrAfterDate(
        currency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuoteEntity>

    fun findAllByCurrencyAndQuoteDateBetween(
        currency: String,
        quoteDateFrom: LocalDate,
        quoteDateTo: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>

    fun findPairClosestOnOrBeforeDate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuotePairRates>

    fun findPairClosestOnOrAfterDate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuotePairRates>

    fun countAll(): Mono<Long>
}
