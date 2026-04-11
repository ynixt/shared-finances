package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteListRequestDto
import com.ynixt.sharedfinances.application.web.jobs.SyncExchangeRatesJob
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateProvider
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ExchangeRateIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var exchangeRateQuoteRepository: ExchangeRateQuoteRepository

    @Autowired
    private lateinit var dbClient: DatabaseClient

    @Autowired
    private lateinit var syncExchangeRatesJob: SyncExchangeRatesJob

    @Autowired
    private lateinit var provider: MutableExchangeRateProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var userTestUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        userTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
    }

    @Test
    fun `should sync quotes and expose paginated listing ordered by newest first`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()

            walletItemRepository
                .save(
                    bankAccountEntity(
                        userId = user.id!!,
                        name = "US Wallet",
                        currency = "USD",
                    ),
                ).awaitSingle()

            walletItemRepository
                .save(
                    bankAccountEntity(
                        userId = user.id!!,
                        name = "BR Wallet",
                        currency = "BRL",
                    ),
                ).awaitSingle()

            provider.shouldFail = false
            provider.setQuotes(
                "USD",
                listOf(
                    quote(base = "USD", target = "BRL", date = LocalDate.of(2026, 4, 8), rate = "5.00"),
                ),
            )
            provider.setQuotes(
                "BRL",
                listOf(
                    quote(base = "BRL", target = "USD", date = LocalDate.of(2026, 4, 9), rate = "0.20"),
                ),
            )

            val synced = syncExchangeRatesJob.runOnce().block()
            assertThat(synced).isEqualTo(2)

            webClient
                .post()
                .uri("/exchange-rates/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .bodyValue(
                    ExchangeRateQuoteListRequestDto(
                        pageRequest = CursorPageRequest(size = 10),
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.items.length()")
                .isEqualTo(2)
                .jsonPath("$.items[0].quoteDate")
                .isEqualTo("2026-04-09")
                .jsonPath("$.items[1].quoteDate")
                .isEqualTo("2026-04-08")
                .jsonPath("$.items[0].baseCurrency")
                .isEqualTo("BRL")
                .jsonPath("$.items[0].quoteCurrency")
                .isEqualTo("USD")
        }
    }

    @Test
    fun `should list quotes with cursor pages and server-side pair filter`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()

            walletItemRepository
                .save(
                    bankAccountEntity(
                        userId = user.id!!,
                        name = "US Wallet",
                        currency = "USD",
                    ),
                ).awaitSingle()

            walletItemRepository
                .save(
                    bankAccountEntity(
                        userId = user.id!!,
                        name = "BR Wallet",
                        currency = "BRL",
                    ),
                ).awaitSingle()

            provider.shouldFail = false
            provider.setQuotes(
                "USD",
                listOf(
                    quote(base = "USD", target = "BRL", date = LocalDate.of(2026, 4, 8), rate = "5.00"),
                ),
            )
            provider.setQuotes(
                "BRL",
                listOf(
                    quote(base = "BRL", target = "USD", date = LocalDate.of(2026, 4, 9), rate = "0.20"),
                ),
            )

            syncExchangeRatesJob.runOnce().block()

            exchangeRateQuoteRepository
                .upsertDaily(
                    id = UUID.randomUUID(),
                    source = "integration-test-seed",
                    baseCurrency = "EUR",
                    quoteCurrency = "USD",
                    quoteDate = LocalDate.of(2026, 4, 6),
                    rate = BigDecimal("1.10"),
                    quotedAt = LocalDate.of(2026, 4, 6).atStartOfDay().atOffset(ZoneOffset.UTC),
                    fetchedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ).awaitSingle()

            val firstPageJson =
                webClient
                    .post()
                    .uri("/exchange-rates/list")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .bodyValue(
                        ExchangeRateQuoteListRequestDto(
                            pageRequest = CursorPageRequest(size = 1),
                        ),
                    ).exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!

            val firstPage: Map<String, Any?> =
                objectMapper.readValue(firstPageJson, object : TypeReference<Map<String, Any?>>() {})

            @Suppress("UNCHECKED_CAST")
            val nextCursor = firstPage["nextCursor"] as Map<String, Any>?
            assertThat(nextCursor).isNotNull
            assertThat(firstPage["hasNext"]).isEqualTo(true)

            webClient
                .post()
                .uri("/exchange-rates/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .bodyValue(
                    ExchangeRateQuoteListRequestDto(
                        pageRequest =
                            CursorPageRequest(
                                size = 1,
                                nextCursor = nextCursor,
                            ),
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.items[0].quoteDate")
                .isEqualTo("2026-04-08")

            webClient
                .post()
                .uri("/exchange-rates/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .bodyValue(
                    ExchangeRateQuoteListRequestDto(
                        pageRequest = CursorPageRequest(size = 20),
                        baseCurrency = "USD",
                        quoteCurrency = "BRL",
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.items.length()")
                .isEqualTo(1)
                .jsonPath("$.items[0].baseCurrency")
                .isEqualTo("USD")
                .jsonPath("$.items[0].quoteCurrency")
                .isEqualTo("BRL")
        }
    }

    @Test
    fun `should order same-day quotes by pair and continue with composite cursor`() {
        runBlocking {
            userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()

            exchangeRateQuoteRepository
                .upsertDaily(
                    id = UUID.randomUUID(),
                    source = "integration-test-seed",
                    baseCurrency = "USD",
                    quoteCurrency = "BRL",
                    quoteDate = LocalDate.of(2026, 4, 10),
                    rate = BigDecimal("5.00"),
                    quotedAt = LocalDate.of(2026, 4, 10).atStartOfDay().atOffset(ZoneOffset.UTC),
                    fetchedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ).awaitSingle()
            exchangeRateQuoteRepository
                .upsertDaily(
                    id = UUID.randomUUID(),
                    source = "integration-test-seed",
                    baseCurrency = "EUR",
                    quoteCurrency = "USD",
                    quoteDate = LocalDate.of(2026, 4, 10),
                    rate = BigDecimal("1.10"),
                    quotedAt = LocalDate.of(2026, 4, 10).atStartOfDay().atOffset(ZoneOffset.UTC),
                    fetchedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ).awaitSingle()
            exchangeRateQuoteRepository
                .upsertDaily(
                    id = UUID.randomUUID(),
                    source = "integration-test-seed",
                    baseCurrency = "GBP",
                    quoteCurrency = "USD",
                    quoteDate = LocalDate.of(2026, 4, 10),
                    rate = BigDecimal("1.30"),
                    quotedAt = LocalDate.of(2026, 4, 10).atStartOfDay().atOffset(ZoneOffset.UTC),
                    fetchedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ).awaitSingle()
            exchangeRateQuoteRepository
                .upsertDaily(
                    id = UUID.randomUUID(),
                    source = "integration-test-seed",
                    baseCurrency = "USD",
                    quoteCurrency = "ARS",
                    quoteDate = LocalDate.of(2026, 4, 9),
                    rate = BigDecimal("200.00"),
                    quotedAt = LocalDate.of(2026, 4, 9).atStartOfDay().atOffset(ZoneOffset.UTC),
                    fetchedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ).awaitSingle()

            val firstPageJson =
                webClient
                    .post()
                    .uri("/exchange-rates/list")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .bodyValue(
                        ExchangeRateQuoteListRequestDto(
                            pageRequest = CursorPageRequest(size = 2),
                        ),
                    ).exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!

            val firstPage: Map<String, Any?> =
                objectMapper.readValue(firstPageJson, object : TypeReference<Map<String, Any?>>() {})

            @Suppress("UNCHECKED_CAST")
            val firstItems = firstPage["items"] as List<Map<String, Any?>>
            assertThat(firstItems.map { "${it["baseCurrency"]}/${it["quoteCurrency"]}@${it["quoteDate"]}" })
                .containsExactly("EUR/USD@2026-04-10", "GBP/USD@2026-04-10")
            assertThat(firstPage["hasNext"]).isEqualTo(true)

            @Suppress("UNCHECKED_CAST")
            val nextCursor = firstPage["nextCursor"] as Map<String, Any>?
            assertThat(nextCursor)
                .isNotNull
                .containsEntry("quoteDate", "2026-04-10")
                .containsEntry("baseCurrency", "GBP")
                .containsEntry("quoteCurrency", "USD")

            val secondPageJson =
                webClient
                    .post()
                    .uri("/exchange-rates/list")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .bodyValue(
                        ExchangeRateQuoteListRequestDto(
                            pageRequest =
                                CursorPageRequest(
                                    size = 2,
                                    nextCursor = nextCursor,
                                ),
                        ),
                    ).exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!

            val secondPage: Map<String, Any?> =
                objectMapper.readValue(secondPageJson, object : TypeReference<Map<String, Any?>>() {})

            @Suppress("UNCHECKED_CAST")
            val secondItems = secondPage["items"] as List<Map<String, Any?>>
            assertThat(secondItems.map { "${it["baseCurrency"]}/${it["quoteCurrency"]}@${it["quoteDate"]}" })
                .containsExactly("USD/BRL@2026-04-10", "USD/ARS@2026-04-09")
            assertThat(secondPage["hasNext"]).isEqualTo(false)
        }
    }

    @Test
    fun `should reject partial composite cursor`() {
        runBlocking {
            userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()

            webClient
                .post()
                .uri("/exchange-rates/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .bodyValue(
                    ExchangeRateQuoteListRequestDto(
                        pageRequest =
                            CursorPageRequest(
                                size = 2,
                                nextCursor =
                                    mapOf(
                                        "quoteDate" to "2026-04-10",
                                        "quotedAt" to "2026-04-10T00:00:00Z",
                                        "id" to UUID.randomUUID().toString(),
                                    ),
                            ),
                    ),
                ).exchange()
                .expectStatus()
                .isBadRequest
                .expectBody()
                .jsonPath("$.alternativeMessage")
                .value<String> {
                    assertThat(it).contains(
                        "nextCursor for exchange rate quotes must include quoteDate, baseCurrency, quoteCurrency, quotedAt, and id together, or omit all.",
                    )
                }
        }
    }

    @Test
    fun `should use index-backed plans for default and pair-filter browse queries`() {
        runBlocking {
            seedExplainDataset()

            val defaultPlan =
                explainQuery(
                    sql =
                        """
                        SELECT *
                        FROM exchange_rate_quote
                        WHERE (
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
                        ORDER BY quote_date DESC, base_currency ASC, quote_currency ASC, quoted_at DESC, id DESC
                        LIMIT :limit
                        """.trimIndent(),
                    bindings =
                        mapOf(
                            "cursorQuoteDate" to LocalDate.of(2026, 1, 8),
                            "cursorBaseCurrency" to "B10",
                            "cursorQuoteCurrency" to "Q10",
                            "cursorQuotedAt" to OffsetDateTime.parse("2026-01-08T12:00:00Z"),
                            "cursorId" to UUID.fromString("11111111-1111-1111-1111-111111111111"),
                            "limit" to 21,
                        ),
                )

            assertThat(defaultPlan)
                .contains("idx_exchange_rate_quote_list_cursor")
                .doesNotContain("Sort")
                .doesNotContain("Offset")

            val pairPlan =
                explainQuery(
                    sql =
                        """
                        SELECT *
                        FROM exchange_rate_quote
                        WHERE
                            base_currency = :baseCurrency
                            AND quote_currency = :quoteCurrency
                            AND (
                                quote_date < :cursorQuoteDate
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
                        ORDER BY quote_date DESC, base_currency ASC, quote_currency ASC, quoted_at DESC, id DESC
                        LIMIT :limit
                        """.trimIndent(),
                    bindings =
                        mapOf(
                            "baseCurrency" to "B10",
                            "quoteCurrency" to "Q10",
                            "cursorQuoteDate" to LocalDate.of(2026, 1, 8),
                            "cursorBaseCurrency" to "B10",
                            "cursorQuoteCurrency" to "Q10",
                            "cursorQuotedAt" to OffsetDateTime.parse("2026-01-08T12:00:00Z"),
                            "cursorId" to UUID.fromString("22222222-2222-2222-2222-222222222222"),
                            "limit" to 21,
                        ),
                )

            assertThat(pairPlan)
                .contains("idx_exchange_rate_quote_pair_list_cursor")
                .doesNotContain("Sort")
                .doesNotContain("Offset")
        }
    }

    @Test
    fun `should keep previous quotes unchanged when sync fails`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()

            walletItemRepository
                .save(
                    bankAccountEntity(
                        userId = user.id!!,
                        name = "US Wallet",
                        currency = "USD",
                    ),
                ).awaitSingle()

            walletItemRepository
                .save(
                    bankAccountEntity(
                        userId = user.id!!,
                        name = "BR Wallet",
                        currency = "BRL",
                    ),
                ).awaitSingle()

            provider.shouldFail = false
            provider.setQuotes(
                "USD",
                listOf(
                    quote(base = "USD", target = "BRL", date = LocalDate.of(2026, 4, 9), rate = "5.10"),
                ),
            )
            provider.setQuotes(
                "BRL",
                listOf(
                    quote(base = "BRL", target = "USD", date = LocalDate.of(2026, 4, 9), rate = "0.19"),
                ),
            )

            syncExchangeRatesJob.runOnce().block()
            val countBeforeFailure = exchangeRateQuoteRepository.countAll().awaitSingle()

            provider.shouldFail = true
            syncExchangeRatesJob.runOnce().onErrorResume { Mono.empty() }.block()

            val countAfterFailure = exchangeRateQuoteRepository.countAll().awaitSingle()
            assertThat(countAfterFailure).isEqualTo(countBeforeFailure)
        }
    }

    private fun bankAccountEntity(
        userId: UUID,
        name: String,
        currency: String,
    ): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.BANK_ACCOUNT,
            name = name,
            enabled = true,
            userId = userId,
            currency = currency,
            balance = BigDecimal.ZERO,
            totalLimit = null,
            dueDay = null,
            daysBetweenDueAndClosing = null,
            dueOnNextBusinessDay = null,
            showOnDashboard = true,
        ).also {
            it.id = UUID.randomUUID()
        }

    private fun quote(
        base: String,
        target: String,
        date: LocalDate,
        rate: String,
    ): ExchangeRateProvider.Quote =
        ExchangeRateProvider.Quote(
            baseCurrency = base,
            quoteCurrency = target,
            quoteDate = date,
            quotedAt = date.atStartOfDay().atOffset(ZoneOffset.UTC),
            rate = BigDecimal(rate),
        )

    private suspend fun seedExplainDataset() {
        dbClient
            .sql("TRUNCATE TABLE exchange_rate_quote")
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql(
                """
                INSERT INTO exchange_rate_quote (
                    id,
                    source,
                    base_currency,
                    quote_currency,
                    quote_date,
                    rate,
                    quoted_at,
                    fetched_at
                )
                SELECT
                    (
                        substr(md5(i::text), 1, 8) || '-' ||
                        substr(md5(i::text), 9, 4) || '-' ||
                        substr(md5(i::text), 13, 4) || '-' ||
                        substr(md5(i::text), 17, 4) || '-' ||
                        substr(md5(i::text), 21, 12)
                    )::uuid,
                    'integration-test-explain',
                    'B' || lpad(((i % 20) + 1)::text, 2, '0'),
                    'Q' || lpad((((i / 20) % 20) + 1)::text, 2, '0'),
                    DATE '2026-01-01' + ((i / 400)::int),
                    1.00 + ((i % 7)::numeric / 100),
                    TIMESTAMPTZ '2026-01-01T00:00:00Z' + make_interval(days => (i / 400)::int, hours => (i % 24)),
                    TIMESTAMPTZ '2026-01-01T00:05:00Z' + make_interval(days => (i / 400)::int, hours => (i % 24))
                FROM generate_series(0, 3999) AS s(i)
                """.trimIndent(),
            ).fetch()
            .rowsUpdated()
            .awaitSingle()
    }

    private suspend fun explainQuery(
        sql: String,
        bindings: Map<String, Any>,
    ): String {
        var spec = dbClient.sql("EXPLAIN (COSTS OFF) $sql")
        for ((name, value) in bindings) {
            spec = spec.bind(name, value)
        }
        return spec
            .map { row, _ -> row.get(0, String::class.java)!! }
            .all()
            .collectList()
            .awaitSingle()
            .joinToString("\n")
    }

    @TestConfiguration
    class ExchangeRateIntegrationTestConfig {
        @Bean
        @Primary
        fun mutableExchangeRateProvider(): MutableExchangeRateProvider = MutableExchangeRateProvider()
    }

    class MutableExchangeRateProvider : ExchangeRateProvider {
        private val quotesByBase = mutableMapOf<String, List<ExchangeRateProvider.Quote>>()
        var shouldFail: Boolean = false

        override val source: String = "integration-test-provider"

        override suspend fun fetchLatest(
            baseCurrency: String,
            quoteCurrencies: Set<String>,
        ): List<ExchangeRateProvider.Quote> {
            if (shouldFail) {
                throw IllegalStateException("forced provider failure")
            }

            return quotesByBase[baseCurrency.uppercase()]
                ?.filter { quoteCurrencies.contains(it.quoteCurrency.uppercase()) }
                .orEmpty()
        }

        override suspend fun fetchForDate(
            baseCurrency: String,
            quoteCurrencies: Set<String>,
            date: java.time.LocalDate,
        ): List<ExchangeRateProvider.Quote> = fetchLatest(baseCurrency, quoteCurrencies)

        fun setQuotes(
            baseCurrency: String,
            quotes: List<ExchangeRateProvider.Quote>,
        ) {
            quotesByBase[baseCurrency.uppercase()] = quotes
        }
    }
}
