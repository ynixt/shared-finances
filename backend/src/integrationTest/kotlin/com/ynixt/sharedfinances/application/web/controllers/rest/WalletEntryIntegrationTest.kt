package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.CursorPageDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.BankAccountTestUtil
import com.ynixt.sharedfinances.support.util.CreditCardTestUtil
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WalletEntryIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var recurrenceEventRepository: RecurrenceEventRepository

    @Autowired
    private lateinit var creditCardBillRepository: CreditCardBillRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var userTestUtil: UserTestUtil
    private lateinit var bankAccountTestUtil: BankAccountTestUtil
    private lateinit var creditCardTestUtil: CreditCardTestUtil

    @BeforeEach
    fun setup() {
        userTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
        bankAccountTestUtil =
            BankAccountTestUtil(
                walletItemRepository = walletItemRepository,
                balance = BigDecimal("1000.00"),
            )
        creditCardTestUtil =
            CreditCardTestUtil(
                walletItemRepository = walletItemRepository,
                balance = BigDecimal("3000.00"),
            )
    }

    @Test
    fun `should create expense entry for current date and decrease bank account balance`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val bankAccount = bankAccountTestUtil.createBankAccountOnDatabase(user.id!!)
            val bankAccountId = bankAccount.id!!
            val accessToken = userTestUtil.login()

            val today = LocalDate.now()
            val expenseValue = BigDecimal("150.00")
            val expectedBalance = bankAccount.balance.subtract(expenseValue)

            val request =
                NewEntryDto(
                    type = WalletEntryType.EXPENSE,
                    groupId = null,
                    originId = bankAccountId,
                    targetId = null,
                    name = "Supermercado",
                    categoryId = null,
                    date = today,
                    value = expenseValue,
                    confirmed = true,
                    observations = "Compra do dia",
                    paymentType = PaymentType.UNIQUE,
                    installments = null,
                    periodicity = null,
                    periodicityQtyLimit = null,
                    originBillDate = null,
                    targetBillDate = null,
                    tags = listOf("mercado", "alimentacao"),
                )

            webClient
                .post()
                .uri("/wallet-entries")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isNoContent
                .expectBody()
                .isEmpty

            val updatedBankAccount = walletItemRepository.findById(bankAccountId).awaitSingle()

            assertThat(updatedBankAccount.type).isEqualTo(WalletItemType.BANK_ACCOUNT)
            assertThat(updatedBankAccount.balance).isEqualByComparingTo(expectedBalance)
            assertThat(updatedBankAccount.name).isEqualTo(bankAccount.name)
            assertThat(updatedBankAccount.currency).isEqualTo(bankAccount.currency)

            val recurrences =
                recurrenceEventRepository
                    .findAll(
                        minimumEndExecution = null,
                        maximumNextExecution = null,
                        billDate = null,
                        walletItemId = bankAccountId,
                        userId = user.id,
                        groupId = null,
                    ).collectList()
                    .awaitSingle()

            assertThat(recurrences).isEmpty()

            val listRequest =
                ListEntryRequestDto(
                    walletItemId = bankAccountId,
                    groupId = null,
                    pageRequest = null,
                    minimumDate = today,
                    maximumDate = today,
                    billId = null,
                    billDate = null,
                )

            val listResponseBody =
                webClient
                    .post()
                    .uri("/wallet-entries/list")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(listRequest)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .returnResult()
                    .responseBody

            assertThat(listResponseBody).isNotNull()
            val listResponseBodyNonNull = requireNotNull(listResponseBody)
            val listResponse = objectMapper.readValue<CursorPageDto<EventForListDto>>(listResponseBodyNonNull)

            assertThat(listResponse.items).hasSize(1)
            assertThat(listResponse.nextCursor).isNull()
            assertThat(listResponse.hasNext).isFalse()

            val event = listResponse.items.first()

            assertThat(event.type).isEqualTo(WalletEntryType.EXPENSE)
            assertThat(event.name).isEqualTo("Supermercado")
            assertThat(event.date).isEqualTo(today)
            assertThat(event.confirmed).isTrue()
            assertThat(event.observations).isEqualTo("Compra do dia")
            assertThat(event.currency).isEqualTo(bankAccount.currency)
            assertThat(event.group).isNull()
            assertThat(event.category).isNull()
            assertThat(event.recurrenceConfigId).isNull()
            assertThat(event.recurrenceConfig).isNull()
            assertThat(event.tags).isNotNull()
            assertThat(event.tags!!).containsExactlyInAnyOrder("mercado", "alimentacao")
            assertThat(event.entries).hasSize(1)

            val entry = event.entries.first()
            val walletItem = entry.walletItem

            assertThat(entry.walletItemId).isEqualTo(bankAccountId)
            assertThat(entry.billDate).isNull()
            assertThat(entry.billId).isNull()
            assertThat(entry.value).isEqualByComparingTo(expenseValue.unaryMinus())
            assertThat(walletItem.type).isEqualTo(WalletItemType.BANK_ACCOUNT)
            assertThat(walletItem.id).isEqualTo(bankAccountId)
            assertThat(walletItem.name).isEqualTo(bankAccount.name)
            assertThat(walletItem.currency).isEqualTo(bankAccount.currency)
        }
    }

    @Test
    fun `should create expense entry for current date on credit card and increment value only on correct bill`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()

            val totalLimit = BigDecimal("3000.00")

            val creditCard = creditCardTestUtil.createCreditCardOnDatabase(user.id!!)
            val creditCardId = creditCard.id!!

            val today = LocalDate.now()
            val expenseValue = BigDecimal("150.00")
            val expectedAvailableLimit = totalLimit.subtract(expenseValue)

            val creditCardModel =
                CreditCard(
                    name = creditCard.name,
                    enabled = creditCard.enabled,
                    userId = creditCard.userId,
                    currency = creditCard.currency,
                    totalLimit = requireNotNull(creditCard.totalLimit),
                    balance = creditCard.balance,
                    dueDay = requireNotNull(creditCard.dueDay),
                    daysBetweenDueAndClosing = requireNotNull(creditCard.daysBetweenDueAndClosing),
                    dueOnNextBusinessDay = requireNotNull(creditCard.dueOnNextBusinessDay),
                )

            val expectedBillDate = creditCardModel.getBestBill(today)
            val expectedDueDate = creditCardModel.getDueDate(expectedBillDate)
            val expectedClosingDate = creditCardModel.getClosingDate(expectedDueDate)
            val initialExpectedBillValue = BigDecimal("-20.00")
            val expectedBillValue = initialExpectedBillValue.add(expenseValue.unaryMinus())

            val expectedBill =
                creditCardBillRepository
                    .save(
                        CreditCardBillEntity(
                            creditCardId = creditCardId,
                            billDate = expectedBillDate,
                            dueDate = expectedDueDate,
                            closingDate = expectedClosingDate,
                            paid = false,
                            value = initialExpectedBillValue,
                        ),
                    ).awaitSingle()

            val anotherBillDate = expectedBillDate.minusMonths(1)
            val anotherDueDate = creditCardModel.getDueDate(anotherBillDate)
            val anotherClosingDate = creditCardModel.getClosingDate(anotherDueDate)
            val initialAnotherBillValue = BigDecimal("-35.00")

            val anotherBill =
                creditCardBillRepository
                    .save(
                        CreditCardBillEntity(
                            creditCardId = creditCardId,
                            billDate = anotherBillDate,
                            dueDate = anotherDueDate,
                            closingDate = anotherClosingDate,
                            paid = false,
                            value = initialAnotherBillValue,
                        ),
                    ).awaitSingle()

            val request =
                NewEntryDto(
                    type = WalletEntryType.EXPENSE,
                    groupId = null,
                    originId = creditCardId,
                    targetId = null,
                    name = "Jantar",
                    categoryId = null,
                    date = today,
                    value = expenseValue,
                    confirmed = true,
                    observations = "Despesa no cartao",
                    paymentType = PaymentType.UNIQUE,
                    installments = null,
                    periodicity = null,
                    periodicityQtyLimit = null,
                    originBillDate = expectedBillDate,
                    targetBillDate = null,
                    tags = listOf("cartao", "alimentacao"),
                )

            webClient
                .post()
                .uri("/wallet-entries")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isNoContent
                .expectBody()
                .isEmpty

            val updatedCreditCard = walletItemRepository.findById(creditCardId).awaitSingle()
            val updatedExpectedBill =
                creditCardBillRepository
                    .findOneByCreditCardIdAndBillDate(
                        creditCardId = creditCardId,
                        billDate = expectedBillDate,
                    ).awaitSingle()
            val updatedAnotherBill = creditCardBillRepository.findById(anotherBill.id!!).awaitSingle()

            assertThat(updatedCreditCard.type).isEqualTo(WalletItemType.CREDIT_CARD)
            assertThat(updatedCreditCard.balance).isEqualByComparingTo(expectedAvailableLimit)
            assertThat(updatedCreditCard.totalLimit).isEqualByComparingTo(totalLimit)
            assertThat(updatedExpectedBill.id).isEqualTo(expectedBill.id)
            assertThat(updatedExpectedBill.value).isEqualByComparingTo(expectedBillValue)
            assertThat(updatedAnotherBill.value).isEqualByComparingTo(initialAnotherBillValue)

            val recurrences =
                recurrenceEventRepository
                    .findAll(
                        minimumEndExecution = null,
                        maximumNextExecution = null,
                        billDate = expectedBillDate,
                        walletItemId = creditCardId,
                        userId = user.id,
                        groupId = null,
                    ).collectList()
                    .awaitSingle()

            assertThat(recurrences).isEmpty()

            val listRequest =
                ListEntryRequestDto(
                    walletItemId = creditCardId,
                    groupId = null,
                    pageRequest = null,
                    minimumDate = today,
                    maximumDate = today,
                    billId = expectedBill.id,
                    billDate = expectedBillDate,
                )

            val listResponseBody =
                webClient
                    .post()
                    .uri("/wallet-entries/list")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(listRequest)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .returnResult()
                    .responseBody

            assertThat(listResponseBody).isNotNull()
            val listResponseBodyNonNull = requireNotNull(listResponseBody)
            val listResponse = objectMapper.readValue<CursorPageDto<EventForListDto>>(listResponseBodyNonNull)

            assertThat(listResponse.items).hasSize(1)
            assertThat(listResponse.nextCursor).isNull()
            assertThat(listResponse.hasNext).isFalse()

            val event = listResponse.items.first()
            assertThat(event.type).isEqualTo(WalletEntryType.EXPENSE)
            assertThat(event.name).isEqualTo("Jantar")
            assertThat(event.date).isEqualTo(today)
            assertThat(event.confirmed).isTrue()
            assertThat(event.observations).isEqualTo("Despesa no cartao")
            assertThat(event.currency).isEqualTo(creditCard.currency)
            assertThat(event.group).isNull()
            assertThat(event.category).isNull()
            assertThat(event.recurrenceConfigId).isNull()
            assertThat(event.recurrenceConfig).isNull()
            assertThat(event.tags).isNotNull()
            assertThat(event.tags!!).containsExactlyInAnyOrder("cartao", "alimentacao")
            assertThat(event.entries).hasSize(1)

            val entry = event.entries.first()
            val walletItem = entry.walletItem

            assertThat(entry.walletItemId).isEqualTo(creditCardId)
            assertThat(entry.billDate).isNull()
            assertThat(entry.billId).isEqualTo(expectedBill.id)
            assertThat(entry.value).isEqualByComparingTo(expenseValue.unaryMinus())
            assertThat(walletItem.type).isEqualTo(WalletItemType.CREDIT_CARD)
            assertThat(walletItem.id).isEqualTo(creditCardId)
            assertThat(walletItem.name).isEqualTo(creditCard.name)
            assertThat(walletItem.currency).isEqualTo(creditCard.currency)
        }
    }

    @Test
    fun `should create missing bill when creating expense entry for current date on credit card`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val creditCard = creditCardTestUtil.createCreditCardOnDatabase(user.id!!)
            val creditCardId = creditCard.id!!

            val today = LocalDate.now()
            val expenseValue = BigDecimal("150.00")
            val expectedAvailableLimit = creditCard.balance.subtract(expenseValue)

            val creditCardModel =
                CreditCard(
                    name = creditCard.name,
                    enabled = creditCard.enabled,
                    userId = creditCard.userId,
                    currency = creditCard.currency,
                    totalLimit = requireNotNull(creditCard.totalLimit),
                    balance = creditCard.balance,
                    dueDay = requireNotNull(creditCard.dueDay),
                    daysBetweenDueAndClosing = requireNotNull(creditCard.daysBetweenDueAndClosing),
                    dueOnNextBusinessDay = requireNotNull(creditCard.dueOnNextBusinessDay),
                )

            val expectedBillDate = creditCardModel.getBestBill(today)
            val expectedDueDate = creditCardModel.getDueDate(expectedBillDate)
            val expectedClosingDate = creditCardModel.getClosingDate(expectedDueDate)

            val billBeforeRequest =
                creditCardBillRepository
                    .findOneByCreditCardIdAndBillDate(
                        creditCardId = creditCardId,
                        billDate = expectedBillDate,
                    ).awaitSingleOrNull()

            assertThat(billBeforeRequest).isNull()

            val request =
                NewEntryDto(
                    type = WalletEntryType.EXPENSE,
                    groupId = null,
                    originId = creditCardId,
                    targetId = null,
                    name = "Farmacia",
                    categoryId = null,
                    date = today,
                    value = expenseValue,
                    confirmed = true,
                    observations = "Compra sem fatura pre-criada",
                    paymentType = PaymentType.UNIQUE,
                    installments = null,
                    periodicity = null,
                    periodicityQtyLimit = null,
                    originBillDate = expectedBillDate,
                    targetBillDate = null,
                    tags = listOf("cartao", "farmacia"),
                )

            webClient
                .post()
                .uri("/wallet-entries")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isNoContent
                .expectBody()
                .isEmpty

            val updatedCreditCard = walletItemRepository.findById(creditCardId).awaitSingle()
            val createdBill =
                creditCardBillRepository
                    .findOneByCreditCardIdAndBillDate(
                        creditCardId = creditCardId,
                        billDate = expectedBillDate,
                    ).awaitSingle()

            assertThat(updatedCreditCard.type).isEqualTo(WalletItemType.CREDIT_CARD)
            assertThat(updatedCreditCard.balance).isEqualByComparingTo(expectedAvailableLimit)
            assertThat(createdBill.creditCardId).isEqualTo(creditCardId)
            assertThat(createdBill.billDate).isEqualTo(expectedBillDate)
            assertThat(createdBill.dueDate).isEqualTo(expectedDueDate)
            assertThat(createdBill.closingDate).isEqualTo(expectedClosingDate)
            assertThat(createdBill.paid).isFalse()
            assertThat(createdBill.value).isEqualByComparingTo(expenseValue.unaryMinus())

            val listRequest =
                ListEntryRequestDto(
                    walletItemId = creditCardId,
                    groupId = null,
                    pageRequest = null,
                    minimumDate = today,
                    maximumDate = today,
                    billId = createdBill.id,
                    billDate = expectedBillDate,
                )

            val listResponseBody =
                webClient
                    .post()
                    .uri("/wallet-entries/list")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(listRequest)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .returnResult()
                    .responseBody

            assertThat(listResponseBody).isNotNull()
            val listResponseBodyNonNull = requireNotNull(listResponseBody)
            val listResponse = objectMapper.readValue<CursorPageDto<EventForListDto>>(listResponseBodyNonNull)

            assertThat(listResponse.items).hasSize(1)

            val event = listResponse.items.first()
            val entry = event.entries.first()

            assertThat(event.type).isEqualTo(WalletEntryType.EXPENSE)
            assertThat(event.name).isEqualTo("Farmacia")
            assertThat(entry.walletItemId).isEqualTo(creditCardId)
            assertThat(entry.billId).isEqualTo(createdBill.id)
            assertThat(entry.value).isEqualByComparingTo(expenseValue.unaryMinus())
        }
    }
}
