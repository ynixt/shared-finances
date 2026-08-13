package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuotePairRates
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface ExchangeRateQuoteSpringDataRepository :
    ExchangeRateQuoteRepository,
    R2dbcRepository<ExchangeRateQuoteEntity, String> {
    @Modifying
    @Query(
        """
            INSERT INTO exchange_rate_quote(
                id,
                source,
                currency,
                quote_date,
                rate,
                fetched_at
            )
            VALUES (
                :id,
                :source,
                :currency,
                :quoteDate,
                :rate,
                :fetchedAt
            )
            ON CONFLICT (currency, quote_date)
            DO UPDATE
            SET
                source = EXCLUDED.source,
                rate = EXCLUDED.rate,
                fetched_at = EXCLUDED.fetched_at,
                updated_at = CURRENT_TIMESTAMP
        """,
    )
    override fun upsertDaily(
        id: UUID,
        source: String,
        currency: String,
        quoteDate: LocalDate,
        rate: BigDecimal,
        fetchedAt: OffsetDateTime,
    ): Mono<Long>

    @Query(
        """
            SELECT *
            FROM exchange_rate_quote
            WHERE
                currency = :currency
                AND quote_date <= :referenceDate
            ORDER BY quote_date DESC
            LIMIT 1
        """,
    )
    override fun findClosestOnOrBeforeDate(
        currency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuoteEntity>

    @Query(
        """
            SELECT *
            FROM exchange_rate_quote
            WHERE
                currency = :currency
                AND quote_date >= :referenceDate
            ORDER BY quote_date ASC
            LIMIT 1
        """,
    )
    override fun findClosestOnOrAfterDate(
        currency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuoteEntity>

    @Query(
        """
            SELECT *
            FROM exchange_rate_quote
            WHERE
                currency = :currency
                AND quote_date >= :quoteDateFrom
                AND quote_date <= :quoteDateTo
            ORDER BY quote_date ASC
        """,
    )
    override fun findAllByCurrencyAndQuoteDateBetween(
        currency: String,
        quoteDateFrom: LocalDate,
        quoteDateTo: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>

    @Query(
        """
            SELECT
                f.quote_date,
                f.rate AS rate_from,
                t.rate AS rate_to
            FROM exchange_rate_quote f
            JOIN exchange_rate_quote t ON t.quote_date = f.quote_date
            WHERE
                f.currency = :fromCurrency
                AND t.currency = :toCurrency
                AND f.quote_date <= :referenceDate
            ORDER BY f.quote_date DESC
            LIMIT 1
        """,
    )
    override fun findPairClosestOnOrBeforeDate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuotePairRates>

    @Query(
        """
            SELECT
                f.quote_date,
                f.rate AS rate_from,
                t.rate AS rate_to
            FROM exchange_rate_quote f
            JOIN exchange_rate_quote t ON t.quote_date = f.quote_date
            WHERE
                f.currency = :fromCurrency
                AND t.currency = :toCurrency
                AND f.quote_date >= :referenceDate
            ORDER BY f.quote_date ASC
            LIMIT 1
        """,
    )
    override fun findPairClosestOnOrAfterDate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuotePairRates>

    @Query("SELECT COUNT(*) FROM exchange_rate_quote")
    override fun countAll(): Mono<Long>
}
