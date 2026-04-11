package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.TransferQuoteRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.TransferRateRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletSourceLegDto
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.JsonUtil
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class WalletEntryIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var exchangeRateQuoteRepository: ExchangeRateQuoteRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

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
    fun `should create installment transaction without fk violation`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()

            val bankAccountRequestJson = JsonUtil.readJsonFromResources("mocks/bank-account/new-bank-account-request-200.json")

            webClient
                .post()
                .uri("/bank-accounts")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bankAccountRequestJson)
                .exchange()
                .expectStatus()
                .isOk

            val bankAccountId =
                walletItemRepository
                    .findAllByUserIdAndType(
                        userId = user.id!!,
                        type = WalletItemType.BANK_ACCOUNT,
                        pageable = PageRequest.of(0, 1),
                    ).collectList()
                    .awaitSingle()
                    .single()
                    .id!!

            val response =
                webClient
                    .post()
                    .uri("/wallet-entries")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        NewEntryDto(
                            type = WalletEntryType.EXPENSE,
                            groupId = null,
                            originId = null,
                            targetId = null,
                            sources =
                                listOf(
                                    WalletSourceLegDto(
                                        walletItemId = bankAccountId,
                                        contributionPercent = BigDecimal("100.00"),
                                        billDate = null,
                                    ),
                                ),
                            name = "Installment Purchase",
                            categoryId = null,
                            date = LocalDate.of(2026, 4, 10),
                            value = BigDecimal("300.00"),
                            originValue = null,
                            targetValue = null,
                            confirmed = true,
                            observations = null,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = null,
                            originBillDate = null,
                            targetBillDate = null,
                            tags = listOf("integration-test"),
                        ),
                    ).exchange()
                    .expectBody(String::class.java)
                    .returnResult()

            assertThat(response.status)
                .describedAs(response.responseBody)
                .isEqualTo(HttpStatus.NO_CONTENT)
        }
    }

    @Test
    fun `should suggest transfer target value from stored quote`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val originId = createBankAccount(accessToken = accessToken, userId = user.id!!, name = "US Wallet", currency = "USD")
            val targetId = createBankAccount(accessToken = accessToken, userId = user.id!!, name = "BR Wallet", currency = "BRL")

            exchangeRateQuoteRepository
                .upsertDaily(
                    id = UUID.randomUUID(),
                    source = "integration-test",
                    baseCurrency = "USD",
                    quoteCurrency = "BRL",
                    quoteDate = LocalDate.of(2026, 4, 10),
                    rate = BigDecimal("5.40"),
                    quotedAt = LocalDate.of(2026, 4, 10).atStartOfDay().atOffset(ZoneOffset.UTC),
                    fetchedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ).awaitSingle()

            val response =
                webClient
                    .post()
                    .uri("/wallet-entries/transfer-quote")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        TransferQuoteRequestDto(
                            groupId = null,
                            originId = originId,
                            targetId = targetId,
                            date = LocalDate.of(2026, 4, 10),
                            originValue = BigDecimal("100.00"),
                        ),
                    ).exchange()
                    .expectBody(String::class.java)
                    .returnResult()

            assertThat(response.status)
                .describedAs(response.responseBody)
                .isEqualTo(HttpStatus.OK)
            assertThat(response.responseBody).contains("\"targetValue\":540.00")
        }
    }

    @Test
    fun `should resolve transfer exchange rate from stored quote`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val originId = createBankAccount(accessToken = accessToken, userId = user.id!!, name = "US Wallet 2", currency = "USD")
            val targetId = createBankAccount(accessToken = accessToken, userId = user.id!!, name = "BR Wallet 2", currency = "BRL")

            exchangeRateQuoteRepository
                .upsertDaily(
                    id = UUID.randomUUID(),
                    source = "integration-test",
                    baseCurrency = "USD",
                    quoteCurrency = "BRL",
                    quoteDate = LocalDate.of(2026, 4, 10),
                    rate = BigDecimal("5.40"),
                    quotedAt = LocalDate.of(2026, 4, 10).atStartOfDay().atOffset(ZoneOffset.UTC),
                    fetchedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ).awaitSingle()

            val response =
                webClient
                    .post()
                    .uri("/wallet-entries/transfer-rate")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        TransferRateRequestDto(
                            groupId = null,
                            originId = originId,
                            targetId = targetId,
                            date = LocalDate.of(2026, 4, 10),
                        ),
                    ).exchange()
                    .expectBody(String::class.java)
                    .returnResult()

            assertThat(response.status)
                .describedAs(response.responseBody)
                .isEqualTo(HttpStatus.OK)
            assertThat(response.responseBody).contains("\"rate\":5.4")
            assertThat(response.responseBody).contains("\"quoteDate\":\"2026-04-10\"")
            assertThat(response.responseBody).contains("\"baseCurrency\":\"USD\"")
            assertThat(response.responseBody).contains("\"quoteCurrency\":\"BRL\"")
        }
    }

    private suspend fun createBankAccount(
        accessToken: String,
        userId: UUID,
        name: String,
        currency: String,
    ): UUID {
        webClient
            .post()
            .uri("/bank-accounts")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "name" to name,
                    "balance" to BigDecimal.ZERO,
                    "currency" to currency,
                    "showOnDashboard" to true,
                ),
            ).exchange()
            .expectStatus()
            .isOk

        return walletItemRepository
            .findAllByUserIdAndType(
                userId = userId,
                type = WalletItemType.BANK_ACCOUNT,
                pageable = PageRequest.of(0, 10),
            ).collectList()
            .awaitSingle()
            .single { it.name == name }
            .id!!
    }
}
