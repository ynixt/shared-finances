package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
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
                base_currency,
                quote_currency,
                quote_date,
                rate,
                quoted_at,
                fetched_at
            )
            VALUES (
                :id,
                :source,
                :baseCurrency,
                :quoteCurrency,
                :quoteDate,
                :rate,
                :quotedAt,
                :fetchedAt
            )
            ON CONFLICT (base_currency, quote_currency, quote_date)
            DO UPDATE
            SET
                source = EXCLUDED.source,
                rate = EXCLUDED.rate,
                quoted_at = EXCLUDED.quoted_at,
                fetched_at = EXCLUDED.fetched_at,
                updated_at = CURRENT_TIMESTAMP
        """,
    )
    override fun upsertDaily(
        id: UUID,
        source: String,
        baseCurrency: String,
        quoteCurrency: String,
        quoteDate: LocalDate,
        rate: BigDecimal,
        quotedAt: OffsetDateTime,
        fetchedAt: OffsetDateTime,
    ): Mono<Long>

    @Query(
        """
            SELECT *
            FROM exchange_rate_quote
            WHERE
                base_currency = :baseCurrency
                AND quote_currency = :quoteCurrency
                AND quote_date <= :referenceDate
            ORDER BY quote_date DESC, quoted_at DESC
            LIMIT 1
        """,
    )
    override fun findClosestOnOrBeforeDate(
        baseCurrency: String,
        quoteCurrency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuoteEntity>

    @Query(
        """
            SELECT *
            FROM exchange_rate_quote
            WHERE
                base_currency = :baseCurrency
                AND quote_currency = :quoteCurrency
                AND quote_date >= :referenceDate
            ORDER BY quote_date ASC, quoted_at DESC
            LIMIT 1
        """,
    )
    override fun findClosestOnOrAfterDate(
        baseCurrency: String,
        quoteCurrency: String,
        referenceDate: LocalDate,
    ): Mono<ExchangeRateQuoteEntity>

    @Query("SELECT COUNT(*) FROM exchange_rate_quote")
    override fun countAll(): Mono<Long>
}
