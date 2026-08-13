package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuote
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteBatchRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteKeysetRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteListCursor
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteUpsert
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateDerivation
import io.r2dbc.spi.Row
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ExchangeRateQuoteDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : DatabaseClientRepository(),
    ExchangeRateQuoteKeysetRepository,
    ExchangeRateQuoteBatchRepository {
    override fun upsertDailyBatch(quotes: Collection<ExchangeRateQuoteUpsert>): Mono<Long> {
        if (quotes.isEmpty()) return Mono.just(0)

        val values =
            quotes.indices.joinToString(", ") { index ->
                "(:id$index, :source$index, :currency$index, :quoteDate$index, :rate$index, :fetchedAt$index)"
            }
        val sql =
            """
            INSERT INTO exchange_rate_quote (id, source, currency, quote_date, rate, fetched_at)
            VALUES $values
            ON CONFLICT (currency, quote_date) DO UPDATE SET
                source = EXCLUDED.source,
                rate = EXCLUDED.rate,
                fetched_at = EXCLUDED.fetched_at,
                updated_at = CURRENT_TIMESTAMP
            """.trimIndent()

        var spec = dbClient.sql(sql)
        quotes.forEachIndexed { index, quote ->
            spec =
                spec
                    .bind("id$index", quote.id)
                    .bind("source$index", quote.source)
                    .bind("currency$index", quote.currency)
                    .bind("quoteDate$index", quote.quoteDate)
                    .bind("rate$index", quote.rate)
                    .bind("fetchedAt$index", quote.fetchedAt)
        }
        return spec.fetch().rowsUpdated()
    }

    override fun findQuotesKeyset(
        limit: Int,
        baseCurrency: String?,
        quoteCurrency: String?,
        quoteDateFrom: LocalDate?,
        quoteDateTo: LocalDate?,
        cursor: ExchangeRateQuoteListCursor?,
    ): Flux<ExchangeRateQuote> {
        val sql =
            StringBuilder(
                """
                SELECT
                    q.source,
                    q.fetched_at,
                    b.currency AS base_currency,
                    q.currency AS quote_currency,
                    b.quote_date,
                    b.rate AS base_rate,
                    q.rate AS quote_rate
                FROM exchange_rate_quote b
                JOIN exchange_rate_quote q
                    ON q.quote_date = b.quote_date
                    AND q.currency <> b.currency
                WHERE 1 = 1
                """.trimIndent(),
            )
        if (baseCurrency != null) sql.append(" AND b.currency = :baseCurrency")
        if (quoteCurrency != null) sql.append(" AND q.currency = :quoteCurrency")
        if (quoteDateFrom != null) sql.append(" AND b.quote_date >= :quoteDateFrom")
        if (quoteDateTo != null) sql.append(" AND b.quote_date <= :quoteDateTo")
        if (cursor != null) {
            sql.append(
                """
                 AND (
                    b.quote_date < :cursorQuoteDate
                    OR (b.quote_date = :cursorQuoteDate AND b.currency > :cursorBaseCurrency)
                    OR (
                        b.quote_date = :cursorQuoteDate
                        AND b.currency = :cursorBaseCurrency
                        AND q.currency > :cursorQuoteCurrency
                    )
                )
                """.trimIndent(),
            )
        }
        sql.append(" ORDER BY b.quote_date DESC, b.currency ASC, q.currency ASC LIMIT :limit")

        var spec = dbClient.sql(sql.toString())
        if (baseCurrency != null) spec = spec.bind("baseCurrency", baseCurrency)
        if (quoteCurrency != null) spec = spec.bind("quoteCurrency", quoteCurrency)
        if (quoteDateFrom != null) spec = spec.bind("quoteDateFrom", quoteDateFrom)
        if (quoteDateTo != null) spec = spec.bind("quoteDateTo", quoteDateTo)
        if (cursor != null) {
            spec =
                spec
                    .bind("cursorQuoteDate", cursor.quoteDate)
                    .bind("cursorBaseCurrency", cursor.baseCurrency)
                    .bind("cursorQuoteCurrency", cursor.quoteCurrency)
        }
        spec = spec.bind("limit", limit)

        return spec.map { row, _ -> exchangeRateQuoteFromJoinedRow(row) }.all()
    }

    override fun findClosestOnOrBeforeDateForCurrencies(
        currencies: Set<String>,
        referenceDate: LocalDate,
    ): Flux<ExchangeRateQuoteEntity> = findClosestForCurrencies(currencies, referenceDate, before = true)

    override fun findClosestOnOrAfterDateForCurrencies(
        currencies: Set<String>,
        referenceDate: LocalDate,
    ): Flux<ExchangeRateQuoteEntity> = findClosestForCurrencies(currencies, referenceDate, before = false)

    private fun findClosestForCurrencies(
        currencies: Set<String>,
        referenceDate: LocalDate,
        before: Boolean,
    ): Flux<ExchangeRateQuoteEntity> {
        if (currencies.isEmpty()) return Flux.empty()

        val dateOperator = if (before) "<=" else ">="
        val dateDirection = if (before) "DESC" else "ASC"
        val sql =
            """
            SELECT DISTINCT ON (currency) *
            FROM exchange_rate_quote
            WHERE
                currency = ANY(:currencies)
                AND quote_date $dateOperator :referenceDate
            ORDER BY currency ASC, quote_date $dateDirection
            """.trimIndent()

        val spec =
            dbClient
                .sql(sql)
                .bind("currencies", currencies.sorted().toTypedArray())
                .bind("referenceDate", referenceDate)
        return spec.map { row, _ -> exchangeRateQuoteEntityFromRow(row) }.all()
    }

    override fun findAllByCurrenciesAndQuoteDateBetween(
        currencies: Set<String>,
        quoteDateFrom: LocalDate,
        quoteDateTo: LocalDate,
    ): Flux<ExchangeRateQuoteEntity> {
        if (currencies.isEmpty()) return Flux.empty()

        val sql =
            """
            SELECT *
            FROM exchange_rate_quote
            WHERE
                currency = ANY(:currencies)
                AND quote_date >= :quoteDateFrom
                AND quote_date <= :quoteDateTo
            ORDER BY currency ASC, quote_date ASC
            """.trimIndent()

        val spec =
            dbClient
                .sql(sql)
                .bind("currencies", currencies.sorted().toTypedArray())
                .bind("quoteDateFrom", quoteDateFrom)
                .bind("quoteDateTo", quoteDateTo)
        return spec.map { row, _ -> exchangeRateQuoteEntityFromRow(row) }.all()
    }

    private fun exchangeRateQuoteFromJoinedRow(row: Row): ExchangeRateQuote {
        val baseCurrency = row.get("base_currency", String::class.java)!!
        val baseRate = row.get("base_rate", BigDecimal::class.java)!!
        val quoteRate = row.get("quote_rate", BigDecimal::class.java)!!
        return ExchangeRateQuote(
            source = row.get("source", String::class.java)!!,
            baseCurrency = baseCurrency,
            quoteCurrency = row.get("quote_currency", String::class.java)!!,
            quoteDate = row.get("quote_date", LocalDate::class.java)!!,
            rate = ExchangeRateDerivation.derive(rateFrom = baseRate, rateTo = quoteRate),
            fetchedAt = row.get("fetched_at", OffsetDateTime::class.java)!!,
            derived = baseCurrency != USD,
        )
    }

    private fun exchangeRateQuoteEntityFromRow(row: Row): ExchangeRateQuoteEntity {
        val entity =
            ExchangeRateQuoteEntity(
                source = row.get("source", String::class.java)!!,
                currency = row.get("currency", String::class.java)!!,
                quoteDate = row.get("quote_date", LocalDate::class.java)!!,
                rate = row.get("rate", BigDecimal::class.java)!!,
                fetchedAt = row.get("fetched_at", OffsetDateTime::class.java)!!,
            )
        entity.id = row.get("id", UUID::class.java)
        entity.createdAt = row.get("created_at", OffsetDateTime::class.java)
        entity.updatedAt = row.get("updated_at", OffsetDateTime::class.java)
        return entity
    }

    private companion object {
        const val USD = "USD"
    }
}
