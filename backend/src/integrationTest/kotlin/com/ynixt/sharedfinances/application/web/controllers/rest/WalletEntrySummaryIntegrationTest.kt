package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.SummaryEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletSourceLegDto
import com.ynixt.sharedfinances.domain.enums.EntrySummaryType
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.config.TestClockConfig
import com.ynixt.sharedfinances.support.util.MutableTestClock
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** Asserts POST /wallet-entries/summary matches overview-style rollover: net before the range augments balance; projected totals stay inside the range. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestClockConfig::class)
class WalletEntrySummaryIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var mutableTestClock: MutableTestClock

    private lateinit var userTestUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        mutableTestClock.setDate(LocalDate.of(2026, 4, 13))
        userTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
    }

    @Test
    fun `bank account summary carries prior future month into balance and scopes projected totals to selected range`() {
        runBlocking {
            val incomeUser = userTestUtil.createUserOnDatabase()
            val incomeToken = userTestUtil.login()
            val incomeBankId =
                createBankAccount(
                    accessToken = incomeToken,
                    userId = incomeUser.id!!,
                    name = "Summary test bank income",
                    currency = "BRL",
                    balance = BigDecimal("1000.00"),
                )

            postRecurringEntry(
                accessToken = incomeToken,
                bankAccountId = incomeBankId,
                type = WalletEntryType.REVENUE,
                value = BigDecimal("1000.00"),
                firstDate = LocalDate.of(2026, 5, 1),
            )

            webClient
                .post()
                .uri("/wallet-entries/summary")
                .header(HttpHeaders.AUTHORIZATION, incomeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    SummaryEntryRequestDto(
                        walletItemId = incomeBankId,
                        groupId = null,
                        minimumDate = LocalDate.of(2026, 6, 1),
                        maximumDate = LocalDate.of(2026, 6, 30),
                        summaryType = EntrySummaryType.BANK_ACCOUNT,
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.total.balance")
                .isEqualTo(2000)
                .jsonPath("$.total.revenue")
                .isEqualTo(1000)
                .jsonPath("$.total.expense")
                .isEqualTo(0)
                .jsonPath("$.totalProjected.balance")
                .isEqualTo(1000)
                .jsonPath("$.totalProjected.revenue")
                .isEqualTo(1000)
                .jsonPath("$.totalProjected.expense")
                .isEqualTo(0)
                .jsonPath("$.totalPeriod.balance")
                .isEqualTo(0)
                .jsonPath("$.totalPeriod.revenue")
                .isEqualTo(0)
                .jsonPath("$.totalPeriod.expense")
                .isEqualTo(0)

            val expenseUtil =
                UserTestUtil(
                    webClient = webClient,
                    passwordEncoder = passwordEncoder,
                    userRepository = userRepository,
                )
            val expenseUser = expenseUtil.createUserOnDatabase()
            val expenseToken = expenseUtil.login()
            val expenseBankId =
                createBankAccount(
                    accessToken = expenseToken,
                    userId = expenseUser.id!!,
                    name = "Summary test bank expense",
                    currency = "BRL",
                    balance = BigDecimal("3000.00"),
                )

            postRecurringEntry(
                accessToken = expenseToken,
                bankAccountId = expenseBankId,
                type = WalletEntryType.EXPENSE,
                value = BigDecimal("-500.00"),
                firstDate = LocalDate.of(2026, 5, 1),
            )

            webClient
                .post()
                .uri("/wallet-entries/summary")
                .header(HttpHeaders.AUTHORIZATION, expenseToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    SummaryEntryRequestDto(
                        walletItemId = expenseBankId,
                        groupId = null,
                        minimumDate = LocalDate.of(2026, 6, 1),
                        maximumDate = LocalDate.of(2026, 6, 30),
                        summaryType = EntrySummaryType.BANK_ACCOUNT,
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.total.balance")
                .isEqualTo(2500)
                .jsonPath("$.totalProjected.balance")
                .isEqualTo(-500)
                .jsonPath("$.totalProjected.expense")
                .isEqualTo(500)
                .jsonPath("$.totalProjected.revenue")
                .isEqualTo(0)
                .jsonPath("$.totalPeriod.balance")
                .isEqualTo(0)
                .jsonPath("$.totalPeriod.revenue")
                .isEqualTo(0)
                .jsonPath("$.totalPeriod.expense")
                .isEqualTo(0)
        }
    }

    private suspend fun createBankAccount(
        accessToken: String,
        userId: UUID,
        name: String,
        currency: String,
        balance: BigDecimal,
    ): UUID {
        webClient
            .post()
            .uri("/bank-accounts")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "name" to name,
                    "balance" to balance,
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

    private fun postRecurringEntry(
        accessToken: String,
        bankAccountId: UUID,
        type: WalletEntryType,
        value: BigDecimal,
        firstDate: LocalDate,
    ) {
        webClient
            .post()
            .uri("/wallet-entries")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                NewEntryDto(
                    type = type,
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
                    name = "Recurring summary test",
                    categoryId = null,
                    date = firstDate,
                    value = value,
                    originValue = null,
                    targetValue = null,
                    confirmed = true,
                    observations = null,
                    paymentType = PaymentType.RECURRING,
                    installments = null,
                    periodicity = RecurrenceType.MONTHLY,
                    periodicityQtyLimit = null,
                    originBillDate = null,
                    targetBillDate = null,
                    tags = listOf("wallet-summary-integration"),
                ),
            ).exchange()
            .expectStatus()
            .isNoContent
    }
}
