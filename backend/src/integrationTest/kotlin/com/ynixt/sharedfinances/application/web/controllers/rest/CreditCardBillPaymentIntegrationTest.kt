package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardBillDto
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
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
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreditCardBillPaymentIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

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
    fun `should pay full credit card bill`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val scenario = createOpenBillScenario(user.id!!, accessToken, billAmount = BigDecimal("200.00"))

            webClient
                .post()
                .uri("/credit-card-bills/${scenario.bill.id}/payments")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "bankAccountId" to scenario.bankAccountId,
                        "date" to LocalDate.of(2026, 4, 8),
                        "amount" to BigDecimal("200.00"),
                        "observations" to "full payment",
                    ),
                ).exchange()
                .expectStatus()
                .isNoContent

            val updatedBill = getBill(accessToken = accessToken, creditCardId = scenario.creditCardId)
            assertThat(updatedBill.value).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(updatedBill.status?.name).isEqualTo("PAID")
            assertThat(currentBalanceOf(scenario.bankAccountId)).isEqualByComparingTo(BigDecimal("800.00"))
        }
    }

    @Test
    fun `should pay partial credit card bill`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val scenario = createOpenBillScenario(user.id!!, accessToken, billAmount = BigDecimal("200.00"))

            webClient
                .post()
                .uri("/credit-card-bills/${scenario.bill.id}/payments")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "bankAccountId" to scenario.bankAccountId,
                        "date" to LocalDate.of(2026, 4, 8),
                        "amount" to BigDecimal("60.00"),
                        "observations" to "partial payment",
                    ),
                ).exchange()
                .expectStatus()
                .isNoContent

            val updatedBill = getBill(accessToken = accessToken, creditCardId = scenario.creditCardId)
            assertThat(updatedBill.value).isEqualByComparingTo(BigDecimal("-140.00"))
            assertThat(currentBalanceOf(scenario.bankAccountId)).isEqualByComparingTo(BigDecimal("940.00"))
        }
    }

    @Test
    fun `should reject bill overpayment`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val scenario = createOpenBillScenario(user.id!!, accessToken, billAmount = BigDecimal("200.00"))

            webClient
                .post()
                .uri("/credit-card-bills/${scenario.bill.id}/payments")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "bankAccountId" to scenario.bankAccountId,
                        "date" to LocalDate.of(2026, 4, 8),
                        "amount" to BigDecimal("250.00"),
                        "observations" to "too much",
                    ),
                ).exchange()
                .expectStatus()
                .isBadRequest

            val updatedBill = getBill(accessToken = accessToken, creditCardId = scenario.creditCardId)
            assertThat(updatedBill.value).isEqualByComparingTo(BigDecimal("-200.00"))
            assertThat(currentBalanceOf(scenario.bankAccountId)).isEqualByComparingTo(BigDecimal("1000.00"))
        }
    }

    @Test
    fun `should reject payment with inaccessible bank account`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val scenario = createOpenBillScenario(user.id!!, accessToken, billAmount = BigDecimal("200.00"))

            val otherUserUtil =
                UserTestUtil(
                    userRepository = userRepository,
                    passwordEncoder = passwordEncoder,
                    webClient = webClient,
                    email = "other-${UUID.randomUUID()}@example.com",
                )
            val otherUser = otherUserUtil.createUserOnDatabase()
            val otherAccessToken = otherUserUtil.login()
            val otherBankId = createBankAccount(otherUser.id!!, otherAccessToken)

            webClient
                .post()
                .uri("/credit-card-bills/${scenario.bill.id}/payments")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "bankAccountId" to otherBankId,
                        "date" to LocalDate.of(2026, 4, 8),
                        "amount" to BigDecimal("50.00"),
                    ),
                ).exchange()
                .expectStatus()
                .isBadRequest

            val updatedBill = getBill(accessToken = accessToken, creditCardId = scenario.creditCardId)
            assertThat(updatedBill.value).isEqualByComparingTo(BigDecimal("-200.00"))
            assertThat(currentBalanceOf(scenario.bankAccountId)).isEqualByComparingTo(BigDecimal("1000.00"))
        }
    }

    @Test
    fun `should reject payment for inaccessible bill`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val scenario = createOpenBillScenario(user.id!!, accessToken, billAmount = BigDecimal("200.00"))

            val otherUserUtil =
                UserTestUtil(
                    userRepository = userRepository,
                    passwordEncoder = passwordEncoder,
                    webClient = webClient,
                    email = "outsider-${UUID.randomUUID()}@example.com",
                )
            val otherUser = otherUserUtil.createUserOnDatabase()
            val otherAccessToken = otherUserUtil.login()
            val otherBankId = createBankAccount(otherUser.id!!, otherAccessToken)

            webClient
                .post()
                .uri("/credit-card-bills/${scenario.bill.id}/payments")
                .header(HttpHeaders.AUTHORIZATION, otherAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "bankAccountId" to otherBankId,
                        "date" to LocalDate.of(2026, 4, 8),
                        "amount" to BigDecimal("50.00"),
                    ),
                ).exchange()
                .expectStatus()
                .isForbidden
        }
    }

    private suspend fun createOpenBillScenario(
        userId: UUID,
        accessToken: String,
        billAmount: BigDecimal,
    ): BillScenario {
        val bankAccountId = createBankAccount(userId = userId, accessToken = accessToken)
        val creditCardId = createCreditCard(userId = userId, accessToken = accessToken)
        createCreditCardExpense(accessToken = accessToken, creditCardId = creditCardId, amount = billAmount)
        val bill = getBill(accessToken = accessToken, creditCardId = creditCardId)
        return BillScenario(bankAccountId = bankAccountId, creditCardId = creditCardId, bill = bill)
    }

    private suspend fun createBankAccount(
        userId: UUID,
        accessToken: String,
    ): UUID {
        webClient
            .post()
            .uri("/bank-accounts")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "name" to "Main bank",
                    "balance" to BigDecimal("1000.00"),
                    "currency" to "BRL",
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
            .single()
            .id!!
    }

    private suspend fun createCreditCard(
        userId: UUID,
        accessToken: String,
    ): UUID {
        webClient
            .post()
            .uri("/credit-cards")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "name" to "Main card",
                    "currency" to "BRL",
                    "totalLimit" to BigDecimal("1000.00"),
                    "dueDay" to 20,
                    "daysBetweenDueAndClosing" to 10,
                    "dueOnNextBusinessDay" to false,
                    "showOnDashboard" to true,
                ),
            ).exchange()
            .expectStatus()
            .isOk

        return walletItemRepository
            .findAllByUserIdAndType(
                userId = userId,
                type = WalletItemType.CREDIT_CARD,
                pageable = PageRequest.of(0, 10),
            ).collectList()
            .awaitSingle()
            .single()
            .id!!
    }

    private fun createCreditCardExpense(
        accessToken: String,
        creditCardId: UUID,
        amount: BigDecimal,
    ) {
        webClient
            .post()
            .uri("/wallet-entries")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "type" to "EXPENSE",
                    "groupId" to null,
                    "originId" to null,
                    "targetId" to null,
                    "sources" to
                        listOf(
                            mapOf(
                                "walletItemId" to creditCardId,
                                "contributionPercent" to BigDecimal("100.00"),
                                "billDate" to LocalDate.of(2026, 4, 1),
                            ),
                        ),
                    "name" to "Card purchase",
                    "categoryId" to null,
                    "date" to LocalDate.of(2026, 4, 5),
                    "value" to amount,
                    "confirmed" to true,
                    "observations" to null,
                    "paymentType" to "UNIQUE",
                    "installments" to null,
                    "periodicity" to null,
                    "periodicityQtyLimit" to null,
                    "originBillDate" to LocalDate.of(2026, 4, 1),
                    "targetBillDate" to null,
                    "tags" to listOf("payment-it"),
                ),
            ).exchange()
            .expectStatus()
            .isNoContent
    }

    private fun getBill(
        accessToken: String,
        creditCardId: UUID,
    ): CreditCardBillDto {
        val response =
            webClient
                .get()
                .uri("/credit-card-bills/$creditCardId/of/2026/4")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        return objectMapper.readValue(requireNotNull(response))
    }

    private suspend fun currentBalanceOf(bankAccountId: UUID): BigDecimal =
        walletItemRepository.findById(bankAccountId).awaitSingle().balance

    private data class BillScenario(
        val bankAccountId: UUID,
        val creditCardId: UUID,
        val bill: CreditCardBillDto,
    )
}
