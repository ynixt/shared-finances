package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
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
        baseCurrency: String,
        quoteCurrency: String,
        quoteDate: LocalDate,
        rate: BigDecimal,
        quotedAt: OffsetDateTime,
        fetchedAt: OffsetDateTime,
    ): Mono<Long>

    fun findClosestOnOrBeforeDate(
        baseCurrency: String,
        quoteCurrency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuoteEntity>

    fun findClosestOnOrAfterDate(
        baseCurrency: String,
        quoteCurrency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuoteEntity>

    fun findAllByPairAndQuoteDateBetween(
        baseCurrency: String,
        quoteCurrency: String,
        quoteDateFrom: LocalDate,
        quoteDateTo: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>

    fun countAll(): Mono<Long>
}
