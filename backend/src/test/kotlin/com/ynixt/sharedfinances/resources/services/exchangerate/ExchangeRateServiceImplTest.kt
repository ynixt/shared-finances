package com.ynixt.sharedfinances.resources.services.exchangerate

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.ExchangeRateUnavailableException
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuotePair
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteBatchRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteKeysetRepository
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteListCursor
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteRepository
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateProvider
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryUserRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletItemRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExchangeRateServiceImplTest {
    @Test
    fun `should use nearest available quote date for past conversion`() =
        runBlocking {
            val repository = FakeExchangeRateQuoteRepository()
            repository.saveQuote("USD", "BRL", LocalDate.of(2026, 3, 10), "5.00")
            repository.saveQuote("USD", "BRL", LocalDate.of(2026, 3, 13), "6.00")

            val service =
                createService(
                    exchangeRateQuoteRepository = repository,
                    exchangeRateProvider = FakeExchangeRateProvider(),
                )

            val rate =
                service.getRate(
                    fromCurrency = "USD",
                    toCurrency = "BRL",
                    referenceDate = LocalDate.of(2026, 3, 12),
                )

            assertThat(rate).isEqualByComparingTo(BigDecimal("6.00"))
        }

    @Test
    fun `resolveRate should expose quote date of nearest stored row`() =
        runBlocking {
            val repository = FakeExchangeRateQuoteRepository()
            repository.saveQuote("USD", "BRL", LocalDate.of(2026, 3, 10), "5.00")
            repository.saveQuote("USD", "BRL", LocalDate.of(2026, 3, 13), "6.00")

            val service =
                createService(
                    exchangeRateQuoteRepository = repository,
                    exchangeRateProvider = FakeExchangeRateProvider(),
                )

            val resolved: ResolvedExchangeRate =
                service.resolveRate(
                    fromCurrency = "USD",
                    toCurrency = "BRL",
                    referenceDate = LocalDate.of(2026, 3, 12),
                )

            assertThat(resolved.rate).isEqualByComparingTo(BigDecimal("6.00"))
            assertThat(resolved.quoteDate).isEqualTo(LocalDate.of(2026, 3, 13))
        }

    @Test
    fun `should fail explicitly when required pair has no history`() =
        runBlocking {
            val service =
                createService(
                    exchangeRateQuoteRepository = FakeExchangeRateQuoteRepository(),
                    exchangeRateProvider = FakeExchangeRateProvider(),
                )

            assertThatThrownBy {
                runBlocking {
                    service.getRate(
                        fromCurrency = "USD",
                        toCurrency = "BRL",
                        referenceDate = LocalDate.of(2026, 3, 12),
                    )
                }
            }.isInstanceOf(ExchangeRateUnavailableException::class.java)
        }

    @Test
    fun `convertBatch should group quote retrieval by pair instead of per request`() =
        runBlocking {
            val repository = FakeExchangeRateQuoteRepository()
            repository.saveQuote("USD", "BRL", LocalDate.of(2026, 3, 10), "5.00")
            repository.saveQuote("USD", "BRL", LocalDate.of(2026, 3, 13), "6.00")
            repository.saveQuote("USD", "BRL", LocalDate.of(2026, 3, 20), "6.20")

            val service =
                createService(
                    exchangeRateQuoteRepository = repository,
                    exchangeRateProvider = FakeExchangeRateProvider(),
                )

            val requests =
                (1..20).map { idx ->
                    ConversionRequest(
                        value = BigDecimal("10.00"),
                        fromCurrency = "USD",
                        toCurrency = "BRL",
                        referenceDate = LocalDate.of(2026, 3, 10).plusDays((idx % 10).toLong()),
                    )
                }

            val result = service.convertBatch(requests)

            assertThat(result).hasSize(20)
            assertThat(repository.batchClosestBeforeCalls).isEqualTo(1)
            assertThat(repository.batchClosestAfterCalls).isEqualTo(1)
            assertThat(repository.batchWindowCalls).isEqualTo(1)
            assertThat(repository.closestBeforeCalls).isZero()
            assertThat(repository.closestAfterCalls).isZero()
            assertThat(repository.windowCalls).isZero()
        }

    @Test
    fun `should keep one latest-of-day record per pair on repeated syncs`() =
        runBlocking {
            val walletItemRepository = InMemoryWalletItemRepository()
            val userRepository = InMemoryUserRepository()
            val quoteRepository = FakeExchangeRateQuoteRepository()
            val provider = FakeExchangeRateProvider()

            val userId = UUID.randomUUID()
            userRepository
                .insert(
                    UserEntity(
                        email = "sync@test.local",
                        passwordHash = "hash",
                        firstName = "Sync",
                        lastName = "Test",
                        lang = "en",
                        defaultCurrency = "USD",
                        tmz = "UTC",
                        photoUrl = null,
                        emailVerified = true,
                        mfaEnabled = false,
                        totpSecret = null,
                        onboardingDone = true,
                    ).also { it.id = userId },
                ).awaitSingle()

            walletItemRepository.save(
                walletItemEntity(
                    id = UUID.randomUUID(),
                    userId = userId,
                    type = WalletItemType.BANK_ACCOUNT,
                    currency = "USD",
                ),
            )
            walletItemRepository.save(
                walletItemEntity(
                    id = UUID.randomUUID(),
                    userId = userId,
                    type = WalletItemType.BANK_ACCOUNT,
                    currency = "BRL",
                ),
            )

            provider.setQuotes(
                "USD",
                listOf(
                    quote("USD", "BRL", "5.00"),
                ),
            )
            provider.setQuotes(
                "BRL",
                listOf(
                    quote("BRL", "USD", "0.20"),
                ),
            )

            val service =
                ExchangeRateServiceImpl(
                    exchangeRateProvider = provider,
                    exchangeRateQuoteRepository = quoteRepository,
                    exchangeRateQuoteBatchRepository = quoteRepository,
                    exchangeRateQuoteKeysetRepository = quoteRepository,
                    walletItemRepository = walletItemRepository,
                    userRepository = userRepository,
                    clock = fixedClock(),
                    objectMapper =
                        tools.jackson.databind.json.JsonMapper
                            .builder()
                            .build(),
                    fetchDelayMs = 0,
                )

            val firstSyncCount = service.syncLatestQuotes()
            assertThat(firstSyncCount).isEqualTo(2)
            assertThat(quoteRepository.countAll().awaitSingle()).isEqualTo(2L)

            provider.setQuotes(
                "USD",
                listOf(
                    quote("USD", "BRL", "5.20"),
                ),
            )

            val secondSyncCount = service.syncLatestQuotes()
            assertThat(secondSyncCount).isEqualTo(2)
            assertThat(quoteRepository.countAll().awaitSingle()).isEqualTo(2L)

            val latestRate =
                service.getRate(
                    fromCurrency = "USD",
                    toCurrency = "BRL",
                    referenceDate = LocalDate.of(2026, 4, 9),
                )

            assertThat(latestRate).isEqualByComparingTo(BigDecimal("5.20"))
        }

    @Test
    fun `should list quotes by date then pair and continue with composite next cursor`() =
        runBlocking {
            val repository = FakeExchangeRateQuoteRepository()
            repository.saveQuote("USD", "BRL", LocalDate.of(2026, 4, 10), "5.00")
            repository.saveQuote("EUR", "USD", LocalDate.of(2026, 4, 10), "1.10")
            repository.saveQuote("GBP", "USD", LocalDate.of(2026, 4, 10), "1.30")
            repository.saveQuote("USD", "ARS", LocalDate.of(2026, 4, 9), "200.00")

            val service =
                createService(
                    exchangeRateQuoteRepository = repository,
                    exchangeRateProvider = FakeExchangeRateProvider(),
                )

            val firstPage =
                service.listQuotes(
                    ExchangeRateQuoteListRequest(
                        pageRequest = CursorPageRequest(size = 2),
                        baseCurrency = null,
                        quoteCurrency = null,
                        quoteDateFrom = null,
                        quoteDateTo = null,
                    ),
                )

            assertThat(firstPage.items).hasSize(2)
            assertThat(firstPage.items.map { "${it.baseCurrency}/${it.quoteCurrency}@${it.quoteDate}" })
                .containsExactly("EUR/USD@2026-04-10", "GBP/USD@2026-04-10")
            assertThat(firstPage.hasNext).isTrue()
            assertThat(firstPage.nextCursor)
                .isNotNull
                .containsEntry("quoteDate", "2026-04-10")
                .containsEntry("baseCurrency", "GBP")
                .containsEntry("quoteCurrency", "USD")

            val secondPage =
                service.listQuotes(
                    ExchangeRateQuoteListRequest(
                        pageRequest =
                            CursorPageRequest(
                                size = 2,
                                nextCursor = firstPage.nextCursor,
                            ),
                        baseCurrency = null,
                        quoteCurrency = null,
                        quoteDateFrom = null,
                        quoteDateTo = null,
                    ),
                )

            assertThat(secondPage.items).hasSize(2)
            assertThat(secondPage.items.map { "${it.baseCurrency}/${it.quoteCurrency}@${it.quoteDate}" })
                .containsExactly("USD/BRL@2026-04-10", "USD/ARS@2026-04-09")
            assertThat(secondPage.hasNext).isFalse()
        }

    private fun createService(
        exchangeRateQuoteRepository: FakeExchangeRateQuoteRepository,
        exchangeRateProvider: ExchangeRateProvider,
    ): ExchangeRateServiceImpl =
        ExchangeRateServiceImpl(
            exchangeRateProvider = exchangeRateProvider,
            exchangeRateQuoteRepository = exchangeRateQuoteRepository,
            exchangeRateQuoteBatchRepository = exchangeRateQuoteRepository,
            exchangeRateQuoteKeysetRepository = exchangeRateQuoteRepository,
            walletItemRepository = InMemoryWalletItemRepository(),
            userRepository = InMemoryUserRepository(),
            clock = fixedClock(),
            objectMapper =
                tools.jackson.databind.json.JsonMapper
                    .builder()
                    .build(),
            fetchDelayMs = 0,
        )

    private fun quote(
        base: String,
        target: String,
        rate: String,
    ) = ExchangeRateProvider.Quote(
        baseCurrency = base,
        quoteCurrency = target,
        quoteDate = LocalDate.of(2026, 4, 9),
        quotedAt = OffsetDateTime.parse("2026-04-09T12:00:00Z"),
        rate = BigDecimal(rate),
    )

    private fun walletItemEntity(
        id: UUID,
        userId: UUID,
        type: WalletItemType,
        currency: String,
    ): WalletItemEntity =
        WalletItemEntity(
            type = type,
            name = "$type-$currency",
            enabled = true,
            userId = userId,
            currency = currency,
            balance = BigDecimal.ZERO,
            totalLimit = if (type == WalletItemType.CREDIT_CARD) BigDecimal("1000.00") else null,
            dueDay = if (type == WalletItemType.CREDIT_CARD) 10 else null,
            daysBetweenDueAndClosing = if (type == WalletItemType.CREDIT_CARD) 7 else null,
            dueOnNextBusinessDay = if (type == WalletItemType.CREDIT_CARD) true else null,
            showOnDashboard = true,
        ).also { it.id = id }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC)

    private class FakeExchangeRateProvider : ExchangeRateProvider {
        private val quotesByBase = mutableMapOf<String, List<ExchangeRateProvider.Quote>>()

        override val source: String = "fake-provider"

        override suspend fun fetchLatest(
            baseCurrency: String,
            quoteCurrencies: Set<String>,
        ): List<ExchangeRateProvider.Quote> =
            quotesByBase[baseCurrency.uppercase()]
                ?.filter { quoteCurrencies.contains(it.quoteCurrency.uppercase()) }
                .orEmpty()

        override suspend fun fetchForDate(
            baseCurrency: String,
            quoteCurrencies: Set<String>,
            date: LocalDate,
        ): List<ExchangeRateProvider.Quote> = fetchLatest(baseCurrency, quoteCurrencies)

        fun setQuotes(
            baseCurrency: String,
            quotes: List<ExchangeRateProvider.Quote>,
        ) {
            quotesByBase[baseCurrency.uppercase()] = quotes
        }
    }

    private class FakeExchangeRateQuoteRepository :
        ExchangeRateQuoteRepository,
        ExchangeRateQuoteKeysetRepository,
        ExchangeRateQuoteBatchRepository {
        private val byPairAndDate = linkedMapOf<Triple<String, String, LocalDate>, ExchangeRateQuoteEntity>()
        private val byId = linkedMapOf<UUID, ExchangeRateQuoteEntity>()
        var closestBeforeCalls: Int = 0
        var closestAfterCalls: Int = 0
        var windowCalls: Int = 0
        var batchClosestBeforeCalls: Int = 0
        var batchClosestAfterCalls: Int = 0
        var batchWindowCalls: Int = 0

        fun saveQuote(
            base: String,
            target: String,
            quoteDate: LocalDate,
            rate: String,
        ) {
            val entity =
                ExchangeRateQuoteEntity(
                    source = "seed",
                    baseCurrency = base,
                    quoteCurrency = target,
                    quoteDate = quoteDate,
                    rate = BigDecimal(rate),
                    quotedAt = quoteDate.atStartOfDay().atOffset(ZoneOffset.UTC),
                    fetchedAt = quoteDate.atStartOfDay().atOffset(ZoneOffset.UTC),
                ).also { it.id = UUID.randomUUID() }

            byPairAndDate[Triple(base.uppercase(), target.uppercase(), quoteDate)] = entity
            byId[entity.id!!] = entity
        }

        override fun upsertDaily(
            id: UUID,
            source: String,
            baseCurrency: String,
            quoteCurrency: String,
            quoteDate: LocalDate,
            rate: BigDecimal,
            quotedAt: OffsetDateTime,
            fetchedAt: OffsetDateTime,
        ): Mono<Long> {
            val key = Triple(baseCurrency.uppercase(), quoteCurrency.uppercase(), quoteDate)
            val existing = byPairAndDate[key]

            val entity =
                ExchangeRateQuoteEntity(
                    source = source,
                    baseCurrency = key.first,
                    quoteCurrency = key.second,
                    quoteDate = quoteDate,
                    rate = rate,
                    quotedAt = quotedAt,
                    fetchedAt = fetchedAt,
                ).also {
                    it.id = existing?.id ?: id
                }

            byPairAndDate[key] = entity
            byId[entity.id!!] = entity

            return Mono.just(1L)
        }

        override fun findClosestOnOrBeforeDate(
            baseCurrency: String,
            quoteCurrency: String,
            referenceDate: LocalDate,
        ): Mono<ExchangeRateQuoteEntity> {
            closestBeforeCalls += 1
            return Mono.justOrEmpty(
                byPairAndDate.values
                    .filter {
                        it.baseCurrency == baseCurrency.uppercase() &&
                            it.quoteCurrency == quoteCurrency.uppercase() &&
                            !it.quoteDate.isAfter(referenceDate)
                    }.maxByOrNull { it.quoteDate },
            )
        }

        override fun findClosestOnOrAfterDate(
            baseCurrency: String,
            quoteCurrency: String,
            referenceDate: LocalDate,
        ): Mono<ExchangeRateQuoteEntity> {
            closestAfterCalls += 1
            return Mono.justOrEmpty(
                byPairAndDate.values
                    .filter {
                        it.baseCurrency == baseCurrency.uppercase() &&
                            it.quoteCurrency == quoteCurrency.uppercase() &&
                            !it.quoteDate.isBefore(referenceDate)
                    }.minByOrNull { it.quoteDate },
            )
        }

        override fun findAllByPairAndQuoteDateBetween(
            baseCurrency: String,
            quoteCurrency: String,
            quoteDateFrom: LocalDate,
            quoteDateTo: LocalDate,
        ): Flux<ExchangeRateQuoteEntity> {
            windowCalls += 1
            return Flux.fromIterable(
                byPairAndDate.values
                    .filter {
                        it.baseCurrency == baseCurrency.uppercase() &&
                            it.quoteCurrency == quoteCurrency.uppercase() &&
                            !it.quoteDate.isBefore(quoteDateFrom) &&
                            !it.quoteDate.isAfter(quoteDateTo)
                    }.sortedBy { it.quoteDate },
            )
        }

        override fun findClosestOnOrBeforeDateForPairs(
            pairs: Set<ExchangeRateQuotePair>,
            referenceDate: LocalDate,
        ): Flux<ExchangeRateQuoteEntity> {
            batchClosestBeforeCalls += 1
            return Flux.fromIterable(
                pairs.mapNotNull { pair ->
                    byPairAndDate.values
                        .filter {
                            it.baseCurrency == pair.baseCurrency.uppercase() &&
                                it.quoteCurrency == pair.quoteCurrency.uppercase() &&
                                !it.quoteDate.isAfter(referenceDate)
                        }.maxByOrNull { it.quoteDate }
                },
            )
        }

        override fun findClosestOnOrAfterDateForPairs(
            pairs: Set<ExchangeRateQuotePair>,
            referenceDate: LocalDate,
        ): Flux<ExchangeRateQuoteEntity> {
            batchClosestAfterCalls += 1
            return Flux.fromIterable(
                pairs.mapNotNull { pair ->
                    byPairAndDate.values
                        .filter {
                            it.baseCurrency == pair.baseCurrency.uppercase() &&
                                it.quoteCurrency == pair.quoteCurrency.uppercase() &&
                                !it.quoteDate.isBefore(referenceDate)
                        }.minByOrNull { it.quoteDate }
                },
            )
        }

        override fun findAllByPairsAndQuoteDateBetween(
            pairs: Set<ExchangeRateQuotePair>,
            quoteDateFrom: LocalDate,
            quoteDateTo: LocalDate,
        ): Flux<ExchangeRateQuoteEntity> {
            batchWindowCalls += 1
            return Flux.fromIterable(
                byPairAndDate.values
                    .filter { entity ->
                        pairs.any { pair ->
                            entity.baseCurrency == pair.baseCurrency.uppercase() &&
                                entity.quoteCurrency == pair.quoteCurrency.uppercase()
                        } &&
                            !entity.quoteDate.isBefore(quoteDateFrom) &&
                            !entity.quoteDate.isAfter(quoteDateTo)
                    }.sortedWith(
                        compareBy<ExchangeRateQuoteEntity> { it.baseCurrency }
                            .thenBy { it.quoteCurrency }
                            .thenBy { it.quoteDate },
                    ),
            )
        }

        override fun countAll(): Mono<Long> = Mono.just(byPairAndDate.size.toLong())

        override fun findQuotesKeyset(
            limit: Int,
            baseCurrency: String?,
            quoteCurrency: String?,
            quoteDateFrom: LocalDate?,
            quoteDateTo: LocalDate?,
            cursor: ExchangeRateQuoteListCursor?,
        ): Flux<ExchangeRateQuoteEntity> {
            val filtered =
                byPairAndDate.values.filter { entity ->
                    (baseCurrency == null || entity.baseCurrency == baseCurrency) &&
                        (quoteCurrency == null || entity.quoteCurrency == quoteCurrency) &&
                        (quoteDateFrom == null || !entity.quoteDate.isBefore(quoteDateFrom)) &&
                        (quoteDateTo == null || !entity.quoteDate.isAfter(quoteDateTo))
                }
            val sorted =
                filtered.sortedWith(
                    compareByDescending<ExchangeRateQuoteEntity> { it.quoteDate }
                        .thenBy { it.baseCurrency }
                        .thenBy { it.quoteCurrency }
                        .thenByDescending { it.quotedAt }
                        .thenByDescending { it.id!! },
                )
            val window =
                if (cursor == null) {
                    sorted
                } else {
                    sorted.filter { isStrictlyBeforeCursor(it, cursor) }
                }
            return Flux.fromIterable(window.take(limit))
        }

        private fun isStrictlyBeforeCursor(
            row: ExchangeRateQuoteEntity,
            cursor: ExchangeRateQuoteListCursor,
        ): Boolean {
            val c1 = row.quoteDate.compareTo(cursor.quoteDate)
            if (c1 != 0) return c1 < 0
            val c2 = row.baseCurrency.compareTo(cursor.baseCurrency)
            if (c2 != 0) return c2 > 0
            val c3 = row.quoteCurrency.compareTo(cursor.quoteCurrency)
            if (c3 != 0) return c3 > 0
            val c4 = row.quotedAt.compareTo(cursor.quotedAt)
            if (c4 != 0) return c4 < 0
            return row.id!!.compareTo(cursor.id) < 0
        }

        override fun findById(id: UUID): Mono<ExchangeRateQuoteEntity> = Mono.justOrEmpty(byId[id])

        override fun deleteById(id: UUID): Mono<Long> {
            val current = byId.remove(id) ?: return Mono.just(0L)
            byPairAndDate.remove(Triple(current.baseCurrency, current.quoteCurrency, current.quoteDate))
            return Mono.just(1L)
        }

        override fun existsById(id: UUID): Mono<Boolean> = Mono.just(byId.containsKey(id))

        override fun <S : ExchangeRateQuoteEntity> save(entity: S): Mono<S> {
            val id = entity.id ?: UUID.randomUUID()
            entity.id = id
            byId[id] = entity
            byPairAndDate[Triple(entity.baseCurrency, entity.quoteCurrency, entity.quoteDate)] = entity
            return Mono.just(entity)
        }

        override fun <S : ExchangeRateQuoteEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

        override fun findAllByIdIn(id: Collection<UUID>): Flux<ExchangeRateQuoteEntity> = Flux.fromIterable(id.mapNotNull { byId[it] })
    }
}
