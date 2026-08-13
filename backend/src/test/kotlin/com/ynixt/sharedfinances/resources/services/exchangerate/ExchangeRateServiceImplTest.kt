package com.ynixt.sharedfinances.resources.services.exchangerate

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.exceptions.http.ExchangeRateUnavailableException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidExchangeRateQuoteCursorException
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuote
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuotePairRates
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteBatchRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteKeysetRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteUpsert
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExchangeRateServiceImplTest {
    private val provider = Mockito.mock(ExchangeRateProvider::class.java)
    private val repository = Mockito.mock(ExchangeRateQuoteRepository::class.java)
    private val batchRepository = Mockito.mock(ExchangeRateQuoteBatchRepository::class.java)
    private val keysetRepository = Mockito.mock(ExchangeRateQuoteKeysetRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC)
    private val service = ExchangeRateServiceImpl(provider, repository, batchRepository, keysetRepository, clock)

    @Test
    fun `resolve derives both USD legs on one common date and prefers earlier on a tie`() =
        runTest {
            val referenceDate = LocalDate.of(2026, 8, 10)
            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("BRL", "EUR", referenceDate)).thenReturn(
                Mono.just(pair(referenceDate.minusDays(2), "5.4321", "0.9210")),
            )
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("BRL", "EUR", referenceDate)).thenReturn(
                Mono.just(pair(referenceDate.plusDays(2), "5.5", "0.93")),
            )

            val resolved = service.resolveRate("brl", "eur", referenceDate)

            assertEquals(referenceDate.minusDays(2), resolved.quoteDate)
            assertEquals(BigDecimal("0.9210").divide(BigDecimal("5.4321"), java.math.MathContext.DECIMAL128), resolved.rate)
        }

    @Test
    fun `resolve reports unavailable when no common date exists`() =
        runTest {
            val date = LocalDate.of(2026, 8, 10)
            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("BRL", "EUR", date)).thenReturn(Mono.empty())
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("BRL", "EUR", date)).thenReturn(Mono.empty())

            assertFailsWith<ExchangeRateUnavailableException> {
                service.resolveRate("BRL", "EUR", date)
            }
        }

    @Test
    fun `USD legs reciprocal extreme pair and same currency remain exact and non-zero`() =
        runTest {
            val date = LocalDate.of(2026, 8, 11)
            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("USD", "BRL", date)).thenReturn(
                Mono.just(pair(date, "1", "5.25")),
            )
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("USD", "BRL", date)).thenReturn(Mono.empty())
            assertEquals(BigDecimal("5.25"), service.getRate("USD", "BRL", date))

            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("BRL", "USD", date)).thenReturn(
                Mono.just(pair(date, "5.25", "1")),
            )
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("BRL", "USD", date)).thenReturn(Mono.empty())
            assertEquals(BigDecimal.ONE.divide(BigDecimal("5.25"), java.math.MathContext.DECIMAL128), service.getRate("BRL", "USD", date))

            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("SHIB", "BTC", date)).thenReturn(
                Mono.just(pair(date, "76000000000", "0.0000094")),
            )
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("SHIB", "BTC", date)).thenReturn(Mono.empty())
            assertTrue(service.getRate("SHIB", "BTC", date) > BigDecimal.ZERO)

            Mockito.clearInvocations(repository)
            val identity = service.resolveRate("EUR", "eur", date)
            assertEquals(BigDecimal.ONE, identity.rate)
            assertEquals(date, identity.quoteDate)
            Mockito.verifyNoInteractions(repository)
        }

    @Test
    fun `batch loads currency series once derives common dates and omits only unavailable requests`() =
        runTest {
            val date = LocalDate.of(2026, 8, 10)
            val currencies = setOf("BRL", "EUR", "JPY")
            Mockito.`when`(batchRepository.findClosestOnOrBeforeDateForCurrencies(currencies, date)).thenReturn(Flux.empty())
            Mockito.`when`(batchRepository.findAllByCurrenciesAndQuoteDateBetween(currencies, date, date)).thenReturn(
                Flux.just(
                    entity("BRL", date, "5"),
                    entity("EUR", date, "0.9"),
                ),
            )
            Mockito.`when`(batchRepository.findClosestOnOrAfterDateForCurrencies(currencies, date)).thenReturn(Flux.empty())
            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("JPY", "EUR", date)).thenReturn(Mono.empty())
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("JPY", "EUR", date)).thenReturn(Mono.empty())
            val available = ConversionRequest(BigDecimal("100"), "BRL", "EUR", date)
            val unavailable = ConversionRequest(BigDecimal("100"), "JPY", "EUR", date)
            val identity = ConversionRequest(BigDecimal("10"), "EUR", "EUR", date)

            val result = service.convertBatch(listOf(available, unavailable, identity))

            assertEquals(BigDecimal("18.00"), result[available])
            assertNull(result[unavailable])
            assertEquals(BigDecimal("10.00"), result[identity])
            Mockito.verify(batchRepository).findAllByCurrenciesAndQuoteDateBetween(currencies, date, date)
        }

    @Test
    fun `batch falls back once per pair when per-currency boundaries have no common date`() =
        runTest {
            val firstDate = LocalDate.of(2026, 8, 10)
            val referenceDate = firstDate.plusDays(1)
            val lastDate = firstDate.plusDays(2)
            val currencies = setOf("BRL", "EUR")
            Mockito.`when`(batchRepository.findClosestOnOrBeforeDateForCurrencies(currencies, referenceDate)).thenReturn(
                Flux.just(entity("BRL", referenceDate, "5.1"), entity("EUR", firstDate, "0.9")),
            )
            Mockito
                .`when`(batchRepository.findAllByCurrenciesAndQuoteDateBetween(currencies, referenceDate, referenceDate))
                .thenReturn(Flux.just(entity("BRL", referenceDate, "5.1")))
            Mockito.`when`(batchRepository.findClosestOnOrAfterDateForCurrencies(currencies, referenceDate)).thenReturn(
                Flux.just(entity("BRL", referenceDate, "5.1"), entity("EUR", lastDate, "0.92")),
            )
            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("BRL", "EUR", referenceDate)).thenReturn(
                Mono.just(pair(firstDate, "5", "0.9")),
            )
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("BRL", "EUR", referenceDate)).thenReturn(
                Mono.just(pair(lastDate, "5.2", "0.92")),
            )
            val firstRequest = ConversionRequest(BigDecimal("100"), "BRL", "EUR", referenceDate)
            val secondRequest = ConversionRequest(BigDecimal("50"), "brl", "eur", referenceDate)

            val converted = service.convertBatch(listOf(firstRequest, secondRequest))
            val individuallyResolved = service.resolveRate("BRL", "EUR", referenceDate)

            assertEquals(firstDate, individuallyResolved.quoteDate)
            assertEquals(BigDecimal("18.00"), converted[firstRequest])
            assertEquals(BigDecimal("9.00"), converted[secondRequest])
            Mockito
                .verify(repository, Mockito.times(2))
                .findPairClosestOnOrBeforeDate("BRL", "EUR", referenceDate)
            Mockito
                .verify(repository, Mockito.times(2))
                .findPairClosestOnOrAfterDate("BRL", "EUR", referenceDate)
        }

    @Test
    fun `batch falls back when loaded common boundary date hides a nearer common date`() =
        runTest {
            val referenceDate = LocalDate.of(2026, 8, 10)
            val nearerCommonDate = referenceDate.minusDays(3)
            val brlOnlyDate = referenceDate.minusDays(1)
            val fartherCommonDate = referenceDate.plusDays(7)
            val currencies = setOf("BRL", "EUR")
            Mockito.`when`(batchRepository.findClosestOnOrBeforeDateForCurrencies(currencies, referenceDate)).thenReturn(
                Flux.just(
                    entity("BRL", brlOnlyDate, "5.1"),
                    entity("EUR", nearerCommonDate, "0.9"),
                ),
            )
            Mockito
                .`when`(batchRepository.findAllByCurrenciesAndQuoteDateBetween(currencies, referenceDate, referenceDate))
                .thenReturn(Flux.empty())
            Mockito.`when`(batchRepository.findClosestOnOrAfterDateForCurrencies(currencies, referenceDate)).thenReturn(
                Flux.just(
                    entity("BRL", fartherCommonDate, "5.6"),
                    entity("EUR", fartherCommonDate, "1.0"),
                ),
            )
            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("BRL", "EUR", referenceDate)).thenReturn(
                Mono.just(pair(nearerCommonDate, "5", "0.9")),
            )
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("BRL", "EUR", referenceDate)).thenReturn(
                Mono.just(pair(fartherCommonDate, "5.6", "1.0")),
            )
            val firstRequest = ConversionRequest(BigDecimal.ONE, "BRL", "EUR", referenceDate)
            val secondRequest = ConversionRequest(BigDecimal.TEN, "BRL", "EUR", referenceDate)

            val batchResolved = service.resolveRateBatch(listOf(firstRequest, secondRequest))
            Mockito
                .verify(repository, Mockito.times(1))
                .findPairClosestOnOrBeforeDate("BRL", "EUR", referenceDate)
            Mockito
                .verify(repository, Mockito.times(1))
                .findPairClosestOnOrAfterDate("BRL", "EUR", referenceDate)
            val individuallyResolved = service.resolveRate("BRL", "EUR", referenceDate)

            assertEquals(individuallyResolved.quoteDate, batchResolved.getValue(firstRequest)?.quoteDate)
            assertEquals(individuallyResolved.rate, batchResolved.getValue(firstRequest)?.rate)
            assertEquals(individuallyResolved, batchResolved.getValue(secondRequest))
        }

    @Test
    fun `batch normal path keeps repository query count constant for many requests`() =
        runTest {
            val firstDate = LocalDate.of(2026, 8, 10)
            val lastDate = firstDate.plusDays(2)
            val currencies = setOf("BRL", "EUR", "JPY")
            Mockito.`when`(batchRepository.findClosestOnOrBeforeDateForCurrencies(currencies, firstDate)).thenReturn(Flux.empty())
            Mockito.`when`(batchRepository.findAllByCurrenciesAndQuoteDateBetween(currencies, firstDate, lastDate)).thenReturn(
                Flux.just(
                    entity("BRL", firstDate, "5"),
                    entity("EUR", firstDate, "0.9"),
                    entity("JPY", firstDate, "145"),
                    entity("BRL", lastDate, "5.2"),
                    entity("EUR", lastDate, "0.92"),
                    entity("JPY", lastDate, "147"),
                ),
            )
            Mockito.`when`(batchRepository.findClosestOnOrAfterDateForCurrencies(currencies, lastDate)).thenReturn(Flux.empty())
            val requests =
                (1..20).flatMap { value ->
                    listOf(
                        ConversionRequest(BigDecimal(value), "BRL", "EUR", firstDate),
                        ConversionRequest(BigDecimal(value), "JPY", "BRL", lastDate),
                    )
                }

            val resolved = service.resolveRateBatch(requests)

            assertEquals(requests.toSet(), resolved.keys)
            assertTrue(resolved.values.all { it != null })
            Mockito
                .verify(batchRepository, Mockito.times(1))
                .findClosestOnOrBeforeDateForCurrencies(currencies, firstDate)
            Mockito
                .verify(batchRepository, Mockito.times(1))
                .findAllByCurrenciesAndQuoteDateBetween(currencies, firstDate, lastDate)
            Mockito
                .verify(batchRepository, Mockito.times(1))
                .findClosestOnOrAfterDateForCurrencies(currencies, lastDate)
            Mockito.verifyNoInteractions(repository)
        }

    @Test
    fun `unrounded cross-rate round trip returns the original value`() =
        runTest {
            val date = LocalDate.of(2026, 8, 10)
            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("BRL", "EUR", date)).thenReturn(
                Mono.just(pair(date, "5", "0.8")),
            )
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("BRL", "EUR", date)).thenReturn(Mono.empty())
            Mockito.`when`(repository.findPairClosestOnOrBeforeDate("EUR", "BRL", date)).thenReturn(
                Mono.just(pair(date, "0.8", "5")),
            )
            Mockito.`when`(repository.findPairClosestOnOrAfterDate("EUR", "BRL", date)).thenReturn(Mono.empty())
            val original = BigDecimal("123.456789")

            val forward = original.multiply(service.getRate("BRL", "EUR", date))
            val roundTrip = forward.multiply(service.getRate("EUR", "BRL", date))

            assertEquals(0, original.compareTo(roundTrip))
        }

    @Test
    fun `sync makes one provider call stores USD and isolates non-positive currencies`() =
        runTest {
            val date = LocalDate.of(2026, 8, 12)
            Mockito.`when`(provider.source).thenReturn("provider")
            Mockito.`when`(provider.fetchUsdRates(date)).thenReturn(
                listOf(
                    ExchangeRateProvider.Quote("USD", date, BigDecimal.ONE),
                    ExchangeRateProvider.Quote("BRL", date, BigDecimal("5.4")),
                    ExchangeRateProvider.Quote("BAD", date, BigDecimal.ZERO),
                ),
            )
            Mockito.`when`(batchRepository.upsertDailyBatch(anyNonNull())).thenReturn(Mono.just(2))

            assertEquals(2, service.syncQuotesForDate(date))
            Mockito.verify(provider, Mockito.times(1)).fetchUsdRates(date)
            val batchCall =
                Mockito
                    .mockingDetails(batchRepository)
                    .invocations
                    .single { it.method.name == "upsertDailyBatch" }

            @Suppress("UNCHECKED_CAST")
            val upserts = batchCall.arguments.single() as Collection<ExchangeRateQuoteUpsert>
            assertEquals(listOf("USD", "BRL"), upserts.map { it.currency })
            Mockito.verifyNoInteractions(repository)
        }

    @Test
    fun `sync falls back to isolated row writes when the batch statement fails`() =
        runTest {
            val date = LocalDate.of(2026, 8, 12)
            Mockito.`when`(provider.source).thenReturn("provider")
            Mockito.`when`(provider.fetchUsdRates(date)).thenReturn(
                listOf(
                    ExchangeRateProvider.Quote("USD", date, BigDecimal.ONE),
                    ExchangeRateProvider.Quote("BRL", date, BigDecimal("5.4")),
                ),
            )
            Mockito.`when`(batchRepository.upsertDailyBatch(anyNonNull())).thenReturn(Mono.error(IllegalStateException("batch")))
            Mockito
                .`when`(
                    repository.upsertDaily(
                        anyNonNull(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        anyNonNull(),
                        anyNonNull(),
                        anyNonNull(),
                    ),
                ).thenReturn(Mono.just(1), Mono.error(IllegalStateException("row")))

            assertEquals(1, service.syncQuotesForDate(date))
            Mockito.verify(repository, Mockito.times(2)).upsertDaily(
                anyNonNull(),
                Mockito.anyString(),
                Mockito.anyString(),
                anyNonNull(),
                anyNonNull(),
                anyNonNull(),
            )
        }

    @Test
    fun `listing defaults to USD and emits a three field cursor`() =
        runTest {
            val date = LocalDate.of(2026, 8, 12)
            val rows =
                listOf(
                    quote("BRL", date, derived = false),
                    quote("EUR", date, derived = false),
                    quote("JPY", date, derived = false),
                )
            Mockito.`when`(keysetRepository.findQuotesKeyset(3, "USD", null, null, null, null)).thenReturn(Flux.fromIterable(rows))

            val page = service.listQuotes(ExchangeRateQuoteListRequest(CursorPageRequest(size = 2), null, null, null, null))

            assertTrue(page.hasNext)
            assertEquals(2, page.items.size)
            assertEquals(setOf("quoteDate", "baseCurrency", "quoteCurrency"), page.nextCursor?.keys)
            assertFalse(page.items.any { it.baseCurrency == it.quoteCurrency })
        }

    @Test
    fun `quote-only listing does not force USD as base`() =
        runTest {
            Mockito.`when`(keysetRepository.findQuotesKeyset(11, null, "BRL", null, null, null)).thenReturn(Flux.empty())
            service.listQuotes(ExchangeRateQuoteListRequest(CursorPageRequest(), null, "BRL", null, null))
            Mockito.verify(keysetRepository).findQuotesKeyset(11, null, "BRL", null, null, null)
        }

    @Test
    fun `partial listing cursor is rejected`() {
        assertFailsWith<InvalidExchangeRateQuoteCursorException> {
            ExchangeRateQuoteListRequest(
                pageRequest = CursorPageRequest(nextCursor = mapOf("quoteDate" to "2026-08-12")),
                baseCurrency = null,
                quoteCurrency = null,
                quoteDateFrom = null,
                quoteDateTo = null,
            )
        }
    }

    private fun pair(
        date: LocalDate,
        from: String,
        to: String,
    ) = ExchangeRateQuotePairRates(date, BigDecimal(from), BigDecimal(to))

    private fun entity(
        currency: String,
        date: LocalDate,
        rate: String,
    ) = ExchangeRateQuoteEntity(
        source = "provider",
        currency = currency,
        quoteDate = date,
        rate = BigDecimal(rate),
        fetchedAt = OffsetDateTime.now(clock),
    )

    private fun quote(
        currency: String,
        date: LocalDate,
        derived: Boolean,
    ) = ExchangeRateQuote(
        source = "provider",
        baseCurrency = "USD",
        quoteCurrency = currency,
        quoteDate = date,
        rate = BigDecimal.ONE,
        fetchedAt = OffsetDateTime.now(clock),
        derived = derived,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = Mockito.any<T>() ?: null as T
}
