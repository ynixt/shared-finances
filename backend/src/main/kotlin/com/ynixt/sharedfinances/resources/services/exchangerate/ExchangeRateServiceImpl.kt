package com.ynixt.sharedfinances.resources.services.exchangerate

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.exceptions.http.ExchangeRateUnavailableException
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteKeysetRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateProvider
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
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
    private val exchangeRateQuoteKeysetRepository: ExchangeRateQuoteKeysetRepository,
    private val walletItemRepository: WalletItemRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
    private val objectMapper: ObjectMapper,
    @Value("\${app.exchangeRates.fetchDelayMs:500}")
    private val fetchDelayMs: Long,
) : ExchangeRateService {
    private val fixedCurrencies: Set<String> by lazy {
        val json = ClassPathResource("currencies.json").inputStream.use { it.readAllBytes().decodeToString() }
        val map: Map<String, String> = objectMapper.readValue(json)
        map.keys.map { it.uppercase() }.toSet()
    }

    override suspend fun syncLatestQuotes(): Int {
        // #region agent log
        run {
            val wd = Paths.get(System.getProperty("user.dir"))
            val logPath =
                if (wd.fileName.toString() == "backend") wd.parent.resolve("debug-e83397.log") else wd.resolve("debug-e83397.log")
            val line =
                """{"sessionId":"e83397","hypothesisId":"H1","location":"ExchangeRateServiceImpl.syncLatestQuotes","message":"sync start clock","data":{"localDate":"${LocalDate.now(
                    clock,
                )}","clockInstant":"${clock.instant()}"},"timestamp":${System.currentTimeMillis()}}"""
            try {
                Files.writeString(logPath, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            } catch (_: Exception) {
            }
        }
        // #endregion
        val dynamicCurrencies =
            (
                walletItemRepository.findDistinctCurrencies().collectList().awaitSingle() +
                    userRepository.findDistinctDefaultCurrencies().collectList().awaitSingle()
            ).map { it.uppercase() }
                .toSet()

        val currencies = fixedCurrencies + dynamicCurrencies

        if (currencies.size < 2) return 0

        var totalUpserts = 0
        val fetchedAt = OffsetDateTime.now(clock)
        var isFirstFetch = true

        currencies.forEach { baseCurrency ->
            val quoteCurrencies = currencies - baseCurrency

            if (!isFirstFetch) {
                delay(fetchDelayMs)
            }
            isFirstFetch = false

            exchangeRateProvider
                .fetchLatest(baseCurrency = baseCurrency, quoteCurrencies = quoteCurrencies)
                .forEach { quote ->
                    exchangeRateQuoteRepository
                        .upsertDaily(
                            id = UUID.randomUUID(),
                            source = exchangeRateProvider.source,
                            baseCurrency = quote.baseCurrency,
                            quoteCurrency = quote.quoteCurrency,
                            quoteDate = quote.quoteDate,
                            rate = quote.rate,
                            quotedAt = quote.quotedAt,
                            fetchedAt = fetchedAt,
                        ).awaitSingle()

                    totalUpserts += 1
                }
        }

        return totalUpserts
    }

    override suspend fun syncQuotesForDate(
        date: LocalDate,
        baseCurrencies: Set<String>?,
    ): Int {
        val allCurrencies = resolveAllCurrencies()
        val effectiveBaseCurrencies =
            if (baseCurrencies.isNullOrEmpty()) {
                allCurrencies
            } else {
                baseCurrencies.map { it.uppercase() }.toSet().intersect(allCurrencies)
            }

        if (effectiveBaseCurrencies.isEmpty() || allCurrencies.size < 2) return 0

        var totalUpserts = 0
        val fetchedAt = OffsetDateTime.now(clock)
        var isFirstFetch = true

        effectiveBaseCurrencies.forEach { baseCurrency ->
            val quoteCurrencies = allCurrencies - baseCurrency

            if (!isFirstFetch) {
                delay(fetchDelayMs)
            }
            isFirstFetch = false

            exchangeRateProvider
                .fetchForDate(baseCurrency = baseCurrency, quoteCurrencies = quoteCurrencies, date = date)
                .forEach { quote ->
                    exchangeRateQuoteRepository
                        .upsertDaily(
                            id = UUID.randomUUID(),
                            source = exchangeRateProvider.source,
                            baseCurrency = quote.baseCurrency,
                            quoteCurrency = quote.quoteCurrency,
                            quoteDate = quote.quoteDate,
                            rate = quote.rate,
                            quotedAt = quote.quotedAt,
                            fetchedAt = fetchedAt,
                        ).awaitSingle()

                    totalUpserts += 1
                }
        }

        return totalUpserts
    }

    private suspend fun resolveAllCurrencies(): Set<String> {
        val dynamicCurrencies =
            (
                walletItemRepository.findDistinctCurrencies().collectList().awaitSingle() +
                    userRepository.findDistinctDefaultCurrencies().collectList().awaitSingle()
            ).map { it.uppercase() }
                .toSet()

        return fixedCurrencies + dynamicCurrencies
    }

    override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity> {
        val size = request.pageRequest.size
        val limit = size + 1
        val rows =
            exchangeRateQuoteKeysetRepository
                .findQuotesKeyset(
                    limit = limit,
                    baseCurrency = request.baseCurrency,
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
                    "quotedAt" to last.quotedAt.toString(),
                    "id" to last.id.toString(),
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

        val selected = selectQuoteForReference(normalizedFrom, normalizedTo, referenceDate)
        val rate = selected?.rate ?: throw ExchangeRateUnavailableException(normalizedFrom, normalizedTo, referenceDate)
        return ResolvedExchangeRate(rate = rate, quoteDate = selected.quoteDate)
    }

    private suspend fun selectQuoteForReference(
        normalizedFrom: String,
        normalizedTo: String,
        referenceDate: LocalDate,
    ): ExchangeRateQuoteEntity? {
        val before =
            exchangeRateQuoteRepository
                .findClosestOnOrBeforeDate(
                    baseCurrency = normalizedFrom,
                    quoteCurrency = normalizedTo,
                    referenceDate = referenceDate,
                ).awaitSingleOrNull()

        val after =
            exchangeRateQuoteRepository
                .findClosestOnOrAfterDate(
                    baseCurrency = normalizedFrom,
                    quoteCurrency = normalizedTo,
                    referenceDate = referenceDate,
                ).awaitSingleOrNull()

        return when {
            before == null && after == null -> null
            before == null -> after
            after == null -> before
            else -> chooseClosest(referenceDate, before, after)
        }
    }

    override suspend fun convert(
        value: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): BigDecimal {
        val rate = getRate(fromCurrency, toCurrency, referenceDate)
        return value.multiply(rate).setScale(2, RoundingMode.HALF_UP)
    }

    override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> {
        if (requests.isEmpty()) return emptyMap()

        val ratesByKey = linkedMapOf<Triple<String, String, LocalDate>, BigDecimal>()
        val normalizedRequests =
            requests.map { request ->
                Triple(request.fromCurrency.uppercase(), request.toCurrency.uppercase(), request.referenceDate) to request
            }

        val requestsByPair =
            normalizedRequests.groupBy(
                keySelector = { (normalized, _) -> normalized.first to normalized.second },
                valueTransform = { (normalized, _) -> normalized.third },
            )

        requestsByPair.forEach { (pair, referenceDates) ->
            val fromCurrency = pair.first
            val toCurrency = pair.second

            if (fromCurrency == toCurrency) {
                referenceDates.forEach { referenceDate ->
                    ratesByKey[Triple(fromCurrency, toCurrency, referenceDate)] = BigDecimal.ONE
                }
                return@forEach
            }

            val minDate = referenceDates.minOrNull()!!
            val maxDate = referenceDates.maxOrNull()!!

            val before =
                exchangeRateQuoteRepository
                    .findClosestOnOrBeforeDate(
                        baseCurrency = fromCurrency,
                        quoteCurrency = toCurrency,
                        referenceDate = minDate,
                    ).awaitSingleOrNull()
            val inWindow =
                exchangeRateQuoteRepository
                    .findAllByPairAndQuoteDateBetween(
                        baseCurrency = fromCurrency,
                        quoteCurrency = toCurrency,
                        quoteDateFrom = minDate,
                        quoteDateTo = maxDate,
                    ).asFlow()
                    .toList()
            val after =
                exchangeRateQuoteRepository
                    .findClosestOnOrAfterDate(
                        baseCurrency = fromCurrency,
                        quoteCurrency = toCurrency,
                        referenceDate = maxDate,
                    ).awaitSingleOrNull()

            val candidates =
                (inWindow + listOfNotNull(before, after))
                    .associateBy { it.quoteDate }
                    .values
                    .sortedBy { it.quoteDate }

            referenceDates.forEach { referenceDate ->
                val selected =
                    chooseClosestFromSortedCandidates(referenceDate, candidates)
                        ?: throw ExchangeRateUnavailableException(fromCurrency, toCurrency, referenceDate)
                ratesByKey[Triple(fromCurrency, toCurrency, referenceDate)] = selected.rate
            }
        }

        return requests.associateWith { request ->
            val key = Triple(request.fromCurrency.uppercase(), request.toCurrency.uppercase(), request.referenceDate)
            request.value
                .multiply(ratesByKey.getValue(key))
                .setScale(2, RoundingMode.HALF_UP)
        }
    }

    private fun chooseClosestFromSortedCandidates(
        referenceDate: LocalDate,
        sortedCandidates: List<ExchangeRateQuoteEntity>,
    ): ExchangeRateQuoteEntity? {
        if (sortedCandidates.isEmpty()) {
            return null
        }

        var before: ExchangeRateQuoteEntity? = null
        var after: ExchangeRateQuoteEntity? = null

        sortedCandidates.forEach { candidate ->
            if (!candidate.quoteDate.isAfter(referenceDate)) {
                before = candidate
            }
            if (!candidate.quoteDate.isBefore(referenceDate) && after == null) {
                after = candidate
            }
        }

        return when {
            before == null && after == null -> null
            before == null -> after
            after == null -> before
            else -> chooseClosest(referenceDate, before = before!!, after = after!!)
        }
    }

    private fun chooseClosest(
        referenceDate: LocalDate,
        before: ExchangeRateQuoteEntity,
        after: ExchangeRateQuoteEntity,
    ): ExchangeRateQuoteEntity {
        val beforeDistance = ChronoUnit.DAYS.between(before.quoteDate, referenceDate).absoluteValue
        val afterDistance = ChronoUnit.DAYS.between(referenceDate, after.quoteDate).absoluteValue

        return if (beforeDistance <= afterDistance) before else after
    }
}
