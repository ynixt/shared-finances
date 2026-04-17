package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuotePair
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteBatchRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteKeysetRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteListCursor
import io.r2dbc.spi.Row
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
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
    override fun findQuotesKeyset(
        limit: Int,
        baseCurrency: String?,
        quoteCurrency: String?,
        quoteDateFrom: LocalDate?,
        quoteDateTo: LocalDate?,
        cursor: ExchangeRateQuoteListCursor?,
    ): Flux<ExchangeRateQuoteEntity> {
        val sql = StringBuilder("SELECT * FROM exchange_rate_quote WHERE 1=1")
        if (baseCurrency != null) sql.append(" AND base_currency = :baseCurrency")
        if (quoteCurrency != null) sql.append(" AND quote_currency = :quoteCurrency")
        if (quoteDateFrom != null) sql.append(" AND quote_date >= :quoteDateFrom")
        if (quoteDateTo != null) sql.append(" AND quote_date <= :quoteDateTo")
        if (cursor != null) {
            sql.append(
                """
                 AND (
                    quote_date < :cursorQuoteDate
                    OR (quote_date = :cursorQuoteDate AND base_currency > :cursorBaseCurrency)
                    OR (
                        quote_date = :cursorQuoteDate
                        AND base_currency = :cursorBaseCurrency
                        AND quote_currency > :cursorQuoteCurrency
                    )
                    OR (
                        quote_date = :cursorQuoteDate
                        AND base_currency = :cursorBaseCurrency
                        AND quote_currency = :cursorQuoteCurrency
                        AND quoted_at < :cursorQuotedAt
                    )
                    OR (
                        quote_date = :cursorQuoteDate
                        AND base_currency = :cursorBaseCurrency
                        AND quote_currency = :cursorQuoteCurrency
                        AND quoted_at = :cursorQuotedAt
                        AND id < :cursorId
                    )
                )
                """.trimIndent(),
            )
        }
        sql.append(
            " ORDER BY quote_date DESC, base_currency ASC, quote_currency ASC, quoted_at DESC, id DESC LIMIT :limit",
        )

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
                    .bind("cursorQuotedAt", cursor.quotedAt)
                    .bind("cursorId", cursor.id)
        }
        spec = spec.bind("limit", limit)

        return spec.map { row, _ -> exchangeRateQuoteFromRow(row) }.all()
    }

    override fun findClosestOnOrBeforeDateForPairs(
        pairs: Set<ExchangeRateQuotePair>,
        referenceDate: LocalDate,
    ): Flux<ExchangeRateQuoteEntity> {
        if (pairs.isEmpty()) {
            return Flux.empty()
        }

        val (pairPredicate, bindings) = buildPairPredicate(pairs)
        val sql =
            """
            SELECT DISTINCT ON (base_currency, quote_currency) *
            FROM exchange_rate_quote
            WHERE
                ($pairPredicate)
                AND quote_date <= :referenceDate
            ORDER BY base_currency ASC, quote_currency ASC, quote_date DESC, quoted_at DESC
            """.trimIndent()

        var spec = dbClient.sql(sql).bind("referenceDate", referenceDate)
        bindings.forEach { (name, value) -> spec = spec.bind(name, value) }
        return spec.map { row, _ -> exchangeRateQuoteFromRow(row) }.all()
    }

    override fun findClosestOnOrAfterDateForPairs(
        pairs: Set<ExchangeRateQuotePair>,
        referenceDate: LocalDate,
    ): Flux<ExchangeRateQuoteEntity> {
        if (pairs.isEmpty()) {
            return Flux.empty()
        }

        val (pairPredicate, bindings) = buildPairPredicate(pairs)
        val sql =
            """
            SELECT DISTINCT ON (base_currency, quote_currency) *
            FROM exchange_rate_quote
            WHERE
                ($pairPredicate)
                AND quote_date >= :referenceDate
            ORDER BY base_currency ASC, quote_currency ASC, quote_date ASC, quoted_at DESC
            """.trimIndent()

        var spec = dbClient.sql(sql).bind("referenceDate", referenceDate)
        bindings.forEach { (name, value) -> spec = spec.bind(name, value) }
        return spec.map { row, _ -> exchangeRateQuoteFromRow(row) }.all()
    }

    override fun findAllByPairsAndQuoteDateBetween(
        pairs: Set<ExchangeRateQuotePair>,
        quoteDateFrom: LocalDate,
        quoteDateTo: LocalDate,
    ): Flux<ExchangeRateQuoteEntity> {
        if (pairs.isEmpty()) {
            return Flux.empty()
        }

        val (pairPredicate, bindings) = buildPairPredicate(pairs)
        val sql =
            """
            SELECT *
            FROM exchange_rate_quote
            WHERE
                ($pairPredicate)
                AND quote_date >= :quoteDateFrom
                AND quote_date <= :quoteDateTo
            ORDER BY base_currency ASC, quote_currency ASC, quote_date ASC, quoted_at DESC
            """.trimIndent()

        var spec =
            dbClient
                .sql(sql)
                .bind("quoteDateFrom", quoteDateFrom)
                .bind("quoteDateTo", quoteDateTo)
        bindings.forEach { (name, value) -> spec = spec.bind(name, value) }
        return spec.map { row, _ -> exchangeRateQuoteFromRow(row) }.all()
    }

    private fun exchangeRateQuoteFromRow(row: Row): ExchangeRateQuoteEntity {
        val entity =
            ExchangeRateQuoteEntity(
                source = row.get("source", String::class.java)!!,
                baseCurrency = row.get("base_currency", String::class.java)!!,
                quoteCurrency = row.get("quote_currency", String::class.java)!!,
                quoteDate = row.get("quote_date", LocalDate::class.java)!!,
                rate = row.get("rate", BigDecimal::class.java)!!,
                quotedAt = row.get("quoted_at", OffsetDateTime::class.java)!!,
                fetchedAt = row.get("fetched_at", OffsetDateTime::class.java)!!,
            )
        entity.id = row.get("id", UUID::class.java)
        entity.createdAt = row.get("created_at", OffsetDateTime::class.java)
        entity.updatedAt = row.get("updated_at", OffsetDateTime::class.java)
        return entity
    }

    private fun buildPairPredicate(pairs: Set<ExchangeRateQuotePair>): Pair<String, Map<String, String>> {
        val bindings = linkedMapOf<String, String>()
        val predicate =
            pairs
                .sortedWith(compareBy({ it.baseCurrency }, { it.quoteCurrency }))
                .mapIndexed { index, pair ->
                    val baseKey = "baseCurrency$index"
                    val quoteKey = "quoteCurrency$index"
                    bindings[baseKey] = pair.baseCurrency
                    bindings[quoteKey] = pair.quoteCurrency
                    "(base_currency = :$baseKey AND quote_currency = :$quoteKey)"
                }.joinToString(" OR ")

        return predicate to bindings
    }
}
