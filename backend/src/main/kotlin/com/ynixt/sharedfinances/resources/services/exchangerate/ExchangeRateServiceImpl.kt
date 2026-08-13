package com.ynixt.sharedfinances.resources.services.exchangerate

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.exceptions.http.ExchangeRateUnavailableException
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuote
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuotePairRates
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteBatchRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteKeysetRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteUpsert
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateDerivation
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateProvider
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.absoluteValue

@Service
class ExchangeRateServiceImpl(
    private val exchangeRateProvider: ExchangeRateProvider,
    private val exchangeRateQuoteRepository: ExchangeRateQuoteRepository,
    private val exchangeRateQuoteBatchRepository: ExchangeRateQuoteBatchRepository,
    private val exchangeRateQuoteKeysetRepository: ExchangeRateQuoteKeysetRepository,
    private val clock: Clock,
) : ExchangeRateService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun syncLatestQuotes(): Int = upsertQuotes(exchangeRateProvider.fetchUsdRates())

    override suspend fun syncQuotesForDate(date: LocalDate): Int = upsertQuotes(exchangeRateProvider.fetchUsdRates(date))

    private suspend fun upsertQuotes(quotes: List<ExchangeRateProvider.Quote>): Int {
        val fetchedAt = OffsetDateTime.now(clock)
        val validQuotes =
            quotes.mapNotNull { quote ->
                if (quote.rate <= BigDecimal.ZERO) {
                    logger.error(
                        "Discarding invalid USD quote - {} - {}: rate must be greater than zero",
                        quote.currency,
                        quote.quoteDate,
                    )
                    null
                } else {
                    ExchangeRateQuoteUpsert(
                        id = UUID.randomUUID(),
                        source = exchangeRateProvider.source,
                        currency = quote.currency.uppercase(),
                        quoteDate = quote.quoteDate,
                        rate = quote.rate,
                        fetchedAt = fetchedAt,
                    )
                }
            }

        if (validQuotes.isEmpty()) return 0

        return try {
            exchangeRateQuoteBatchRepository.upsertDailyBatch(validQuotes).awaitSingle().toInt()
        } catch (batchException: Exception) {
            logger.error("Error on batch upsert of USD quotes; falling back to row-by-row writes", batchException)
            var totalUpserts = 0
            validQuotes.forEach { quote ->
                try {
                    exchangeRateQuoteRepository
                        .upsertDaily(
                            id = quote.id,
                            source = quote.source,
                            currency = quote.currency,
                            quoteDate = quote.quoteDate,
                            rate = quote.rate,
                            fetchedAt = quote.fetchedAt,
                        ).awaitSingle()
                    totalUpserts += 1
                } catch (ex: Exception) {
                    logger.error(
                        "Error on upsert USD quote - {} - {}: {}",
                        quote.currency,
                        quote.quoteDate,
                        ex.message,
                        ex,
                    )
                }
            }
            totalUpserts
        }
    }

    override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuote> {
        val size = request.pageRequest.size
        val effectiveBaseCurrency = request.baseCurrency ?: if (request.quoteCurrency == null) USD else null
        val rows =
            exchangeRateQuoteKeysetRepository
                .findQuotesKeyset(
                    limit = size + 1,
                    baseCurrency = effectiveBaseCurrency,
                    quoteCurrency = request.quoteCurrency,
                    quoteDateFrom = request.quoteDateFrom,
                    quoteDateTo = request.quoteDateTo,
                    cursor = request.cursor,
                ).asFlow()
                .toList()

        val hasNext = rows.size > size
        val items = if (hasNext) rows.dropLast(1) else rows
        val nextCursor =
            if (!hasNext || items.isEmpty()) {
                null
            } else {
                val last = items.last()
                mapOf(
                    "quoteDate" to last.quoteDate.toString(),
                    "baseCurrency" to last.baseCurrency,
                    "quoteCurrency" to last.quoteCurrency,
                )
            }
        return CursorPage(items = items, nextCursor = nextCursor, hasNext = hasNext)
    }

    override suspend fun getRate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): BigDecimal = resolveRate(fromCurrency, toCurrency, referenceDate).rate

    override suspend fun resolveRate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): ResolvedExchangeRate {
        val normalizedFrom = fromCurrency.uppercase()
        val normalizedTo = toCurrency.uppercase()

        if (normalizedFrom == normalizedTo) {
            return ResolvedExchangeRate(rate = BigDecimal.ONE, quoteDate = referenceDate)
        }

        val selected =
            selectQuoteForReference(normalizedFrom, normalizedTo, referenceDate)
                ?: throw ExchangeRateUnavailableException(normalizedFrom, normalizedTo, referenceDate)
        return ResolvedExchangeRate(
            rate = ExchangeRateDerivation.derive(selected.rateFrom, selected.rateTo),
            quoteDate = selected.quoteDate,
        )
    }

    private suspend fun selectQuoteForReference(
        normalizedFrom: String,
        normalizedTo: String,
        referenceDate: LocalDate,
    ): ExchangeRateQuotePairRates? {
        val before =
            exchangeRateQuoteRepository
                .findPairClosestOnOrBeforeDate(normalizedFrom, normalizedTo, referenceDate)
                .awaitSingleOrNull()
        val after =
            exchangeRateQuoteRepository
                .findPairClosestOnOrAfterDate(normalizedFrom, normalizedTo, referenceDate)
                .awaitSingleOrNull()

        return chooseClosest(referenceDate, before, after) { it.quoteDate }
    }

    override suspend fun convert(
        value: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): BigDecimal =
        value
            .multiply(getRate(fromCurrency, toCurrency, referenceDate))
            .setScale(2, RoundingMode.HALF_UP)

    override suspend fun resolveRateBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, ResolvedExchangeRate?> {
        if (requests.isEmpty()) return emptyMap()

        val normalized =
            requests.map { request ->
                request to
                    NormalizedConversionRequest(
                        fromCurrency = request.fromCurrency.uppercase(),
                        toCurrency = request.toCurrency.uppercase(),
                        referenceDate = request.referenceDate,
                    )
            }
        val nonIdentity = normalized.map { it.second }.filter { it.fromCurrency != it.toCurrency }
        if (nonIdentity.isEmpty()) {
            return normalized.associate { (request, normalizedRequest) ->
                request to ResolvedExchangeRate(BigDecimal.ONE, normalizedRequest.referenceDate)
            }
        }

        val minDate = nonIdentity.minOf { it.referenceDate }
        val maxDate = nonIdentity.maxOf { it.referenceDate }
        val ratesByCurrency =
            loadCurrencySeries(
                currencies = nonIdentity.flatMap { listOf(it.fromCurrency, it.toCurrency) }.toSet(),
                minDate = minDate,
                maxDate = maxDate,
            )
        val pairs = nonIdentity.map { CurrencyPair(it.fromCurrency, it.toCurrency) }.toSet()
        val commonDatesByPair =
            pairs.associateWith { pair ->
                ratesByCurrency[pair.fromCurrency]
                    .orEmpty()
                    .keys
                    .intersect(
                        ratesByCurrency[pair.toCurrency]
                            .orEmpty()
                            .keys,
                    )
            }
        val pairsNeedingFallback =
            nonIdentity
                .groupBy { request -> CurrencyPair(request.fromCurrency, request.toCurrency) }
                .filter { (pair, pairRequests) ->
                    val commonDates = commonDatesByPair[pair].orEmpty()
                    pairRequests.any { request ->
                        val selectedDate = selectClosestDate(request.referenceDate, commonDates)
                        !isProvablyOptimalWithinLoadedRange(selectedDate, request.referenceDate, minDate, maxDate)
                    }
                }.keys
        val fallbackByPair =
            buildMap {
                pairsNeedingFallback.forEach { pair ->
                    put(
                        pair,
                        PairFallback(
                            before =
                                exchangeRateQuoteRepository
                                    .findPairClosestOnOrBeforeDate(pair.fromCurrency, pair.toCurrency, minDate)
                                    .awaitSingleOrNull(),
                            after =
                                exchangeRateQuoteRepository
                                    .findPairClosestOnOrAfterDate(pair.fromCurrency, pair.toCurrency, maxDate)
                                    .awaitSingleOrNull(),
                        ),
                    )
                }
            }

        return normalized.associate { (request, normalizedRequest) ->
            request to resolveNormalizedRequest(normalizedRequest, ratesByCurrency, commonDatesByPair, fallbackByPair)
        }
    }

    override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> {
        val resolvedByRequest = resolveRateBatch(requests)
        return buildMap {
            requests.forEach { request ->
                val resolved = resolvedByRequest[request]
                if (resolved == null) {
                    logger.warn(
                        "Exchange rate unavailable in batch: {} - {} - {}",
                        request.fromCurrency.uppercase(),
                        request.toCurrency.uppercase(),
                        request.referenceDate,
                    )
                } else {
                    put(
                        request,
                        request.value.multiply(resolved.rate).setScale(2, RoundingMode.HALF_UP),
                    )
                }
            }
        }
    }

    private suspend fun loadCurrencySeries(
        currencies: Set<String>,
        minDate: LocalDate,
        maxDate: LocalDate,
    ): Map<String, Map<LocalDate, BigDecimal>> =
        (
            exchangeRateQuoteBatchRepository
                .findClosestOnOrBeforeDateForCurrencies(currencies, minDate)
                .asFlow()
                .toList() +
                exchangeRateQuoteBatchRepository
                    .findAllByCurrenciesAndQuoteDateBetween(currencies, minDate, maxDate)
                    .asFlow()
                    .toList() +
                exchangeRateQuoteBatchRepository
                    .findClosestOnOrAfterDateForCurrencies(currencies, maxDate)
                    .asFlow()
                    .toList()
        ).groupBy(ExchangeRateQuoteEntity::currency)
            .mapValues { (_, rows) -> rows.associate { it.quoteDate to it.rate } }

    private fun resolveNormalizedRequest(
        request: NormalizedConversionRequest,
        ratesByCurrency: Map<String, Map<LocalDate, BigDecimal>>,
        commonDatesByPair: Map<CurrencyPair, Set<LocalDate>>,
        fallbackByPair: Map<CurrencyPair, PairFallback>,
    ): ResolvedExchangeRate? {
        if (request.fromCurrency == request.toCurrency) {
            return ResolvedExchangeRate(BigDecimal.ONE, request.referenceDate)
        }

        val pair = CurrencyPair(request.fromCurrency, request.toCurrency)
        val commonDates = commonDatesByPair[pair].orEmpty()
        val fromRates = ratesByCurrency[request.fromCurrency].orEmpty()
        val toRates = ratesByCurrency[request.toCurrency].orEmpty()
        val inMemoryCandidate =
            selectClosestDate(request.referenceDate, commonDates)?.let { selectedDate ->
                ResolvedExchangeRate(
                    rate =
                        ExchangeRateDerivation.derive(
                            rateFrom = fromRates.getValue(selectedDate),
                            rateTo = toRates.getValue(selectedDate),
                        ),
                    quoteDate = selectedDate,
                )
            }
        val fallback = fallbackByPair[pair] ?: return inMemoryCandidate
        val candidates =
            buildList {
                inMemoryCandidate?.let(::add)
                fallback.before?.let { add(it.toResolvedExchangeRate()) }
                fallback.after?.let { add(it.toResolvedExchangeRate()) }
            }
        val before = candidates.filterNot { it.quoteDate.isAfter(request.referenceDate) }.maxByOrNull { it.quoteDate }
        val after = candidates.filterNot { it.quoteDate.isBefore(request.referenceDate) }.minByOrNull { it.quoteDate }
        return chooseClosest(request.referenceDate, before, after) { it.quoteDate }
    }

    private fun isProvablyOptimalWithinLoadedRange(
        selectedDate: LocalDate?,
        referenceDate: LocalDate,
        minDate: LocalDate,
        maxDate: LocalDate,
    ): Boolean {
        if (selectedDate == null) return false

        val selectedDistance = ChronoUnit.DAYS.between(selectedDate, referenceDate).absoluteValue
        val nearestRangeBoundaryDistance =
            minOf(
                ChronoUnit.DAYS.between(minDate, referenceDate),
                ChronoUnit.DAYS.between(referenceDate, maxDate),
            )
        return selectedDistance <= nearestRangeBoundaryDistance
    }

    private fun ExchangeRateQuotePairRates.toResolvedExchangeRate() =
        ResolvedExchangeRate(
            rate = ExchangeRateDerivation.derive(rateFrom, rateTo),
            quoteDate = quoteDate,
        )

    private fun selectClosestDate(
        referenceDate: LocalDate,
        dates: Set<LocalDate>,
    ): LocalDate? {
        val before = dates.filterNot { it.isAfter(referenceDate) }.maxOrNull()
        val after = dates.filterNot { it.isBefore(referenceDate) }.minOrNull()
        return chooseClosest(referenceDate, before, after) { it }
    }

    private fun <T> chooseClosest(
        referenceDate: LocalDate,
        before: T?,
        after: T?,
        dateOf: (T) -> LocalDate,
    ): T? =
        when {
            before == null && after == null -> null
            before == null -> after
            after == null -> before
            else -> {
                val beforeDistance = ChronoUnit.DAYS.between(dateOf(before), referenceDate).absoluteValue
                val afterDistance = ChronoUnit.DAYS.between(referenceDate, dateOf(after)).absoluteValue
                if (beforeDistance <= afterDistance) before else after
            }
        }

    private data class NormalizedConversionRequest(
        val fromCurrency: String,
        val toCurrency: String,
        val referenceDate: LocalDate,
    )

    private data class CurrencyPair(
        val fromCurrency: String,
        val toCurrency: String,
    )

    private data class PairFallback(
        val before: ExchangeRateQuotePairRates?,
        val after: ExchangeRateQuotePairRates?,
    )

    private companion object {
        const val USD = "USD"
    }
}
