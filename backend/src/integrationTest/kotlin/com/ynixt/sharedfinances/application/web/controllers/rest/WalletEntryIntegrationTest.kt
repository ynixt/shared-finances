package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardBillDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.NewCreditCardDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EditScheduledEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ScheduledExecutionManagerRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletSourceLegDto
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.config.TestClockConfig
import com.ynixt.sharedfinances.support.util.JsonUtil
import com.ynixt.sharedfinances.support.util.MutableTestClock
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestClockConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WalletEntryIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var walletEntryCreateService: WalletEntryCreateService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var mutableTestClock: MutableTestClock

    private lateinit var userTestUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        mutableTestClock.setDate(LocalDate.of(2026, 5, 15))
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
    fun `should keep generated first installment consistent through scheduled edit endpoint`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val creditCardId =
                createCreditCard(
                    userId = user.id!!,
                    accessToken = accessToken,
                    name = "AAA",
                    totalLimit = BigDecimal("1000.00"),
                    dueDay = 1,
                    daysBetweenDueAndClosing = 2,
                )

            createInstallmentExpense(
                accessToken = accessToken,
                creditCardId = creditCardId,
                name = null,
                date = LocalDate.of(2026, 5, 15),
                billDate = LocalDate.of(2026, 6, 1),
                confirmed = true,
                installmentValue = BigDecimal("100.00"),
                installments = 5,
                tags = null,
            )

            val createdEvent =
                getGeneratedWalletEntryByDate(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    date = LocalDate.of(2026, 5, 15),
                )
            val creditCardAfterCreate = getCreditCard(accessToken, creditCardId)
            val createdRecurrence = requireNotNull(createdEvent.recurrenceConfig)
            assertThat(creditCardAfterCreate.balance).isEqualByComparingTo(BigDecimal("500.00"))
            assertThat(createdEvent.date).isEqualTo(LocalDate.of(2026, 5, 15))
            assertThat(createdEvent.installment).isEqualTo(1)
            assertThat(createdRecurrence.qtyExecuted).isEqualTo(1)
            assertThat(createdRecurrence.nextExecution).isEqualTo(LocalDate.of(2026, 6, 15))
            assertThat(createdEvent.entries.single().billDate).isEqualTo(LocalDate.of(2026, 6, 1))

            editScheduledEntry(
                accessToken = accessToken,
                recurrenceConfigId = createdEvent.recurrenceConfigId!!,
                request =
                    EditScheduledEntryDto(
                        occurrenceDate = LocalDate.of(2026, 5, 15),
                        scope = ScheduledEditScope.THIS_AND_FUTURE,
                        entry =
                            NewEntryDto(
                                type = WalletEntryType.EXPENSE,
                                groupId = null,
                                originId = null,
                                targetId = null,
                                sources =
                                    listOf(
                                        WalletSourceLegDto(
                                            walletItemId = creditCardId,
                                            contributionPercent = BigDecimal("100.00"),
                                            billDate = LocalDate.of(2026, 5, 1),
                                        ),
                                    ),
                                beneficiaries = null,
                                name = null,
                                categoryId = null,
                                date = LocalDate.of(2026, 4, 22),
                                value = BigDecimal("100.00"),
                                originValue = null,
                                targetValue = null,
                                confirmed = true,
                                observations = null,
                                paymentType = PaymentType.INSTALLMENTS,
                                installments = 5,
                                periodicity = RecurrenceType.MONTHLY,
                                periodicityQtyLimit = null,
                                originBillDate = null,
                                targetBillDate = null,
                                tags = null,
                                transferPurpose = null,
                            ),
                    ),
            )

            val creditCard = getCreditCard(accessToken, creditCardId)
            val scheduled = listScheduled(accessToken, ScheduledExecutionFilter.ALL)
            val generatedAfterEdit =
                getGeneratedWalletEntryByDate(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    date = LocalDate.of(2026, 4, 22),
                )
            val nextScheduled = scheduled.single { it.id == null }

            assertThat(creditCard.balance).isEqualByComparingTo(BigDecimal("500.00"))
            assertThat(generatedAfterEdit.id).isEqualTo(createdEvent.id)
            assertThat(generatedAfterEdit.entries.single().billDate).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(nextScheduled.date).isEqualTo(LocalDate.of(2026, 5, 22))
            assertThat(nextScheduled.entries.single().billDate).isEqualTo(LocalDate.of(2026, 6, 1))
            assertThat(scheduled.map { it.date })
                .containsExactly(LocalDate.of(2026, 4, 22), LocalDate.of(2026, 5, 22))
        }
    }

    @Test
    fun `should keep future first installment consistent through scheduled edit endpoint`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val creditCardId =
                createCreditCard(
                    userId = user.id!!,
                    accessToken = accessToken,
                    name = "BBB",
                    totalLimit = BigDecimal("1000.00"),
                    dueDay = 1,
                    daysBetweenDueAndClosing = 2,
                )

            createInstallmentExpense(
                accessToken = accessToken,
                creditCardId = creditCardId,
                name = "AAA",
                date = LocalDate.of(2026, 5, 16),
                billDate = LocalDate.of(2026, 6, 1),
                confirmed = false,
                installmentValue = BigDecimal("100.00"),
                installments = 5,
                tags = emptyList(),
            )

            val scheduledBefore = getLatestScheduledEntry(accessToken)
            val scheduledRecurrence = requireNotNull(scheduledBefore.recurrenceConfig)
            assertThat(scheduledBefore.id).isNull()
            assertThat(scheduledBefore.date).isEqualTo(LocalDate.of(2026, 5, 16))
            assertThat(scheduledRecurrence.qtyExecuted).isEqualTo(0)
            assertThat(scheduledRecurrence.nextExecution).isEqualTo(LocalDate.of(2026, 5, 16))
            assertThat(scheduledBefore.entries.single().billDate).isEqualTo(LocalDate.of(2026, 6, 1))

            editScheduledEntry(
                accessToken = accessToken,
                recurrenceConfigId = scheduledBefore.recurrenceConfigId!!,
                request =
                    EditScheduledEntryDto(
                        occurrenceDate = LocalDate.of(2026, 5, 16),
                        scope = ScheduledEditScope.THIS_AND_FUTURE,
                        entry =
                            NewEntryDto(
                                type = WalletEntryType.EXPENSE,
                                groupId = null,
                                originId = null,
                                targetId = null,
                                sources =
                                    listOf(
                                        WalletSourceLegDto(
                                            walletItemId = creditCardId,
                                            contributionPercent = BigDecimal("100.00"),
                                            billDate = LocalDate.of(2026, 5, 1),
                                        ),
                                    ),
                                beneficiaries = null,
                                name = "AAA",
                                categoryId = null,
                                date = LocalDate.of(2026, 4, 22),
                                value = BigDecimal("100.00"),
                                originValue = null,
                                targetValue = null,
                                confirmed = false,
                                observations = null,
                                paymentType = PaymentType.INSTALLMENTS,
                                installments = 5,
                                periodicity = RecurrenceType.MONTHLY,
                                periodicityQtyLimit = null,
                                originBillDate = null,
                                targetBillDate = null,
                                tags = emptyList(),
                                transferPurpose = null,
                            ),
                    ),
            )

            val creditCard = getCreditCard(accessToken, creditCardId)
            val scheduled = listScheduled(accessToken, ScheduledExecutionFilter.ALL)
            val generatedAfterEdit =
                getGeneratedWalletEntryByDate(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    date = LocalDate.of(2026, 4, 22),
                )
            val nextScheduled = scheduled.single { it.id == null }

            assertThat(creditCard.balance).isEqualByComparingTo(BigDecimal("500.00"))
            assertThat(generatedAfterEdit.entries.single().billDate).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(nextScheduled.date).isEqualTo(LocalDate.of(2026, 5, 22))
            assertThat(nextScheduled.entries.single().billDate).isEqualTo(LocalDate.of(2026, 6, 1))
            assertThat(scheduled.map { it.date })
                .containsExactly(LocalDate.of(2026, 4, 22), LocalDate.of(2026, 5, 22))
        }
    }

    @Test
    fun `should persist confirmed flag when scheduled edit materializes occurrence`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val creditCardId =
                createCreditCard(
                    userId = user.id!!,
                    accessToken = accessToken,
                    name = "CCC",
                    totalLimit = BigDecimal("1000.00"),
                    dueDay = 1,
                    daysBetweenDueAndClosing = 2,
                )

            createInstallmentExpense(
                accessToken = accessToken,
                creditCardId = creditCardId,
                name = "AAA",
                date = LocalDate.of(2026, 5, 16),
                billDate = LocalDate.of(2026, 6, 1),
                confirmed = false,
                installmentValue = BigDecimal("100.00"),
                installments = 5,
                tags = emptyList(),
            )

            val scheduledBefore = getLatestScheduledEntry(accessToken)

            editScheduledEntry(
                accessToken = accessToken,
                recurrenceConfigId = scheduledBefore.recurrenceConfigId!!,
                request =
                    EditScheduledEntryDto(
                        occurrenceDate = LocalDate.of(2026, 5, 16),
                        scope = ScheduledEditScope.THIS_AND_FUTURE,
                        entry =
                            NewEntryDto(
                                type = WalletEntryType.EXPENSE,
                                groupId = null,
                                originId = null,
                                targetId = null,
                                sources =
                                    listOf(
                                        WalletSourceLegDto(
                                            walletItemId = creditCardId,
                                            contributionPercent = BigDecimal("100.00"),
                                            billDate = LocalDate.of(2026, 5, 1),
                                        ),
                                    ),
                                beneficiaries = null,
                                name = "AAA",
                                categoryId = null,
                                date = LocalDate.of(2026, 5, 15),
                                value = BigDecimal("100.00"),
                                originValue = null,
                                targetValue = null,
                                confirmed = true,
                                observations = null,
                                paymentType = PaymentType.INSTALLMENTS,
                                installments = 5,
                                periodicity = RecurrenceType.MONTHLY,
                                periodicityQtyLimit = null,
                                originBillDate = null,
                                targetBillDate = null,
                                tags = emptyList(),
                                transferPurpose = null,
                            ),
                    ),
            )

            val generatedAfterEdit =
                getGeneratedWalletEntryByDate(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    date = LocalDate.of(2026, 5, 15),
                )

            assertThat(generatedAfterEdit.confirmed).isTrue()
        }
    }

    @Test
    fun `should keep one installment per bill month after editing generated credit card purchase with business-day due date`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val creditCardId =
                createCreditCard(
                    userId = user.id!!,
                    accessToken = accessToken,
                    name = "AAA",
                    totalLimit = BigDecimal("1000.00"),
                    dueDay = 1,
                    daysBetweenDueAndClosing = 10,
                    dueOnNextBusinessDay = true,
                )

            createInstallmentExpense(
                accessToken = accessToken,
                creditCardId = creditCardId,
                name = "BBB",
                date = LocalDate.of(2026, 5, 15),
                billDate = LocalDate.of(2026, 6, 1),
                confirmed = true,
                installmentValue = BigDecimal("100.00"),
                installments = 5,
                tags = null,
            )

            val createdEvent =
                getGeneratedWalletEntryByDate(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    date = LocalDate.of(2026, 5, 15),
                )
            assertThat(createdEvent.entries.single().billDate).isEqualTo(LocalDate.of(2026, 6, 1))

            editScheduledEntry(
                accessToken = accessToken,
                recurrenceConfigId = createdEvent.recurrenceConfigId!!,
                request =
                    EditScheduledEntryDto(
                        occurrenceDate = LocalDate.of(2026, 5, 15),
                        scope = ScheduledEditScope.THIS_AND_FUTURE,
                        entry =
                            NewEntryDto(
                                type = WalletEntryType.EXPENSE,
                                groupId = null,
                                originId = null,
                                targetId = null,
                                sources =
                                    listOf(
                                        WalletSourceLegDto(
                                            walletItemId = creditCardId,
                                            contributionPercent = BigDecimal("100.00"),
                                            billDate = LocalDate.of(2026, 5, 1),
                                        ),
                                    ),
                                beneficiaries = null,
                                name = "BBB",
                                categoryId = null,
                                date = LocalDate.of(2026, 4, 23),
                                value = BigDecimal("100.00"),
                                originValue = null,
                                targetValue = null,
                                confirmed = true,
                                observations = null,
                                paymentType = PaymentType.INSTALLMENTS,
                                installments = 5,
                                periodicity = RecurrenceType.MONTHLY,
                                periodicityQtyLimit = null,
                                originBillDate = null,
                                targetBillDate = null,
                                tags = null,
                                transferPurpose = null,
                            ),
                    ),
            )

            val scheduledAfterEdit = listScheduled(accessToken, ScheduledExecutionFilter.ALL)
            val firstFutureAfterEdit = scheduledAfterEdit.single { it.id == null }
            assertThat(firstFutureAfterEdit.date).isEqualTo(LocalDate.of(2026, 5, 23))
            assertThat(firstFutureAfterEdit.entries.single().billDate).isEqualTo(LocalDate.of(2026, 6, 1))

            val juneBill = getCreditCardBillForMonth(accessToken, creditCardId, month = 6, year = 2026)
            val julyBill = getCreditCardBillForMonth(accessToken, creditCardId, month = 7, year = 2026)
            val augustBill = getCreditCardBillForMonth(accessToken, creditCardId, month = 8, year = 2026)
            val septemberBill = getCreditCardBillForMonth(accessToken, creditCardId, month = 9, year = 2026)

            val juneEntries =
                listWalletEntriesForBill(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    billDate = LocalDate.of(2026, 6, 1),
                    billId = juneBill.id,
                )
            val julyEntries =
                listWalletEntriesForBill(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    billDate = LocalDate.of(2026, 7, 1),
                    billId = julyBill.id,
                )
            val augustEntries =
                listWalletEntriesForBill(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    billDate = LocalDate.of(2026, 8, 1),
                    billId = augustBill.id,
                )
            val septemberEntries =
                listWalletEntriesForBill(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    billDate = LocalDate.of(2026, 9, 1),
                    billId = septemberBill.id,
                )

            assertThat(juneEntries.map { it.date }).containsExactly(LocalDate.of(2026, 5, 23))
            assertThat(juneEntries.map { it.entries.single().billDate }).containsExactly(LocalDate.of(2026, 6, 1))
            assertThat(julyEntries.map { it.date }).containsExactly(LocalDate.of(2026, 6, 23))
            assertThat(julyEntries.map { it.entries.single().billDate }).containsExactly(LocalDate.of(2026, 7, 1))
            assertThat(augustEntries.map { it.date }).containsExactly(LocalDate.of(2026, 7, 23))
            assertThat(augustEntries.map { it.entries.single().billDate }).containsExactly(LocalDate.of(2026, 8, 1))
            assertThat(septemberEntries.map { it.date }).containsExactly(LocalDate.of(2026, 8, 23))
            assertThat(septemberEntries.map { it.entries.single().billDate }).containsExactly(LocalDate.of(2026, 9, 1))

            val creditCard = getCreditCard(accessToken, creditCardId)
            assertThat(creditCard.balance).isEqualByComparingTo(BigDecimal("500.00"))
        }
    }

    @Test
    fun `should create one installment per bill month for purchase before closing date`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val creditCardId =
                createCreditCard(
                    userId = user.id!!,
                    accessToken = accessToken,
                    name = "AAA",
                    totalLimit = BigDecimal("1000.00"),
                    dueDay = 1,
                    daysBetweenDueAndClosing = 10,
                    dueOnNextBusinessDay = true,
                )

            createInstallmentExpense(
                accessToken = accessToken,
                creditCardId = creditCardId,
                name = "BBB",
                date = LocalDate.of(2026, 5, 15),
                billDate = LocalDate.of(2026, 6, 1),
                confirmed = true,
                installmentValue = BigDecimal("100.00"),
                installments = 5,
                tags = null,
            )

            val createdEvent =
                getGeneratedWalletEntryByDate(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    date = LocalDate.of(2026, 5, 15),
                )

            assertThat(createdEvent.entries.single().billDate).isEqualTo(LocalDate.of(2026, 6, 1))
            assertThat(createdEvent.recurrenceConfig?.nextExecution).isEqualTo(LocalDate.of(2026, 6, 15))

            listOf(
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 9, 15),
            ).forEach { occurrenceDate ->
                walletEntryCreateService.createFromRecurrenceConfig(
                    recurrenceConfigId = createdEvent.recurrenceConfigId!!,
                    date = occurrenceDate,
                )
            }

            val generatedEvents =
                getGeneratedWalletEntriesBetween(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    minimumDate = LocalDate.of(2026, 5, 15),
                    maximumDate = LocalDate.of(2026, 9, 15),
                )

            assertThat(generatedEvents.map { it.date })
                .containsExactly(
                    LocalDate.of(2026, 5, 15),
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 7, 15),
                    LocalDate.of(2026, 8, 15),
                    LocalDate.of(2026, 9, 15),
                )
            assertThat(generatedEvents.map { it.entries.single().billDate })
                .containsExactly(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 1),
                )
            assertThat(generatedEvents.groupingBy { it.entries.single().billDate }.eachCount().values)
                .allMatch { it == 1 }
        }
    }

    @Test
    fun `should create next-cycle first installment when purchase happens on closing day`() {
        runBlocking {
            mutableTestClock.setDate(LocalDate.of(2026, 5, 22))

            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val creditCardId =
                createCreditCard(
                    userId = user.id!!,
                    accessToken = accessToken,
                    name = "AAA",
                    totalLimit = BigDecimal("1000.00"),
                    dueDay = 1,
                    daysBetweenDueAndClosing = 10,
                    dueOnNextBusinessDay = true,
                )

            createInstallmentExpense(
                accessToken = accessToken,
                creditCardId = creditCardId,
                name = "BBB",
                date = LocalDate.of(2026, 5, 22),
                billDate = LocalDate.of(2026, 7, 1),
                confirmed = true,
                installmentValue = BigDecimal("100.00"),
                installments = 5,
                tags = null,
            )

            val createdEvent =
                getGeneratedWalletEntryByDate(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    date = LocalDate.of(2026, 5, 22),
                )

            assertThat(createdEvent.entries.single().billDate).isEqualTo(LocalDate.of(2026, 7, 1))
            assertThat(createdEvent.recurrenceConfig?.nextExecution).isEqualTo(LocalDate.of(2026, 6, 22))

            listOf(
                LocalDate.of(2026, 6, 22),
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 8, 22),
                LocalDate.of(2026, 9, 22),
            ).forEach { occurrenceDate ->
                walletEntryCreateService.createFromRecurrenceConfig(
                    recurrenceConfigId = createdEvent.recurrenceConfigId!!,
                    date = occurrenceDate,
                )
            }

            val generatedEvents =
                getGeneratedWalletEntriesBetween(
                    accessToken = accessToken,
                    walletItemId = creditCardId,
                    minimumDate = LocalDate.of(2026, 5, 22),
                    maximumDate = LocalDate.of(2026, 9, 22),
                )

            assertThat(generatedEvents.map { it.date })
                .containsExactly(
                    LocalDate.of(2026, 5, 22),
                    LocalDate.of(2026, 6, 22),
                    LocalDate.of(2026, 7, 22),
                    LocalDate.of(2026, 8, 22),
                    LocalDate.of(2026, 9, 22),
                )
            assertThat(generatedEvents.map { it.entries.single().billDate })
                .containsExactly(
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 1),
                    LocalDate.of(2026, 11, 1),
                )
            assertThat(generatedEvents.groupingBy { it.entries.single().billDate }.eachCount().values)
                .allMatch { it == 1 }
        }
    }

    private suspend fun createCreditCard(
        userId: UUID,
        accessToken: String,
        name: String,
        totalLimit: BigDecimal,
        dueDay: Int,
        daysBetweenDueAndClosing: Int,
        dueOnNextBusinessDay: Boolean = false,
    ): UUID {
        webClient
            .post()
            .uri("/credit-cards")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                NewCreditCardDto(
                    name = name,
                    currency = "BRL",
                    totalLimit = totalLimit,
                    dueDay = dueDay,
                    daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                    dueOnNextBusinessDay = dueOnNextBusinessDay,
                    showOnDashboard = true,
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
            .single { it.name == name }
            .id!!
    }

    private fun createInstallmentExpense(
        accessToken: String,
        creditCardId: UUID,
        name: String?,
        date: LocalDate,
        billDate: LocalDate,
        confirmed: Boolean,
        installmentValue: BigDecimal,
        installments: Int,
        tags: List<String>?,
    ) {
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
                                walletItemId = creditCardId,
                                contributionPercent = BigDecimal("100.00"),
                                billDate = billDate,
                            ),
                        ),
                    beneficiaries = null,
                    name = name,
                    categoryId = null,
                    date = date,
                    value = installmentValue,
                    originValue = null,
                    targetValue = null,
                    confirmed = confirmed,
                    observations = null,
                    paymentType = PaymentType.INSTALLMENTS,
                    installments = installments,
                    periodicity = RecurrenceType.MONTHLY,
                    periodicityQtyLimit = null,
                    originBillDate = null,
                    targetBillDate = null,
                    tags = tags,
                    transferPurpose = null,
                ),
            ).exchange()
            .expectStatus()
            .isNoContent
    }

    private fun getGeneratedWalletEntryByDate(
        accessToken: String,
        walletItemId: UUID,
        date: LocalDate,
    ): EventForListDto =
        getGeneratedWalletEntriesBetween(
            accessToken = accessToken,
            walletItemId = walletItemId,
            minimumDate = date,
            maximumDate = date,
        ).single()

    private fun getGeneratedWalletEntriesBetween(
        accessToken: String,
        walletItemId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): List<EventForListDto> {
        val response =
            webClient
                .post()
                .uri("/wallet-entries/list")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "walletItemId" to walletItemId,
                        "minimumDate" to minimumDate,
                        "maximumDate" to maximumDate,
                        "pageRequest" to mapOf("size" to 20),
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        val body = requireNotNull(response).decodeToString()
        val itemsNode = objectMapper.readTree(body).get("items")
        return (0 until itemsNode.size())
            .map { index -> objectMapper.treeToValue(itemsNode.get(index), EventForListDto::class.java) }
            .filter { it.id != null }
            .map { getWalletEntryById(accessToken, it.id!!) }
            .sortedBy { it.date }
    }

    private fun getGeneratedWalletEntryIdByDate(
        accessToken: String,
        walletItemId: UUID,
        date: LocalDate,
    ): UUID =
        getGeneratedWalletEntriesBetween(
            accessToken = accessToken,
            walletItemId = walletItemId,
            minimumDate = date,
            maximumDate = date,
        ).single().id!!

    private fun getWalletEntryById(
        accessToken: String,
        walletEventId: UUID,
    ): EventForListDto {
        val response =
            webClient
                .get()
                .uri("/wallet-entries/$walletEventId")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        return objectMapper.readValue(requireNotNull(response), EventForListDto::class.java)
    }

    private fun getCreditCardBillForMonth(
        accessToken: String,
        creditCardId: UUID,
        month: Int,
        year: Int,
    ): CreditCardBillDto {
        val response =
            webClient
                .get()
                .uri("/credit-card-bills/$creditCardId/of/$year/$month")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        return objectMapper.readValue(requireNotNull(response), CreditCardBillDto::class.java)
    }

    private fun listWalletEntriesForBill(
        accessToken: String,
        walletItemId: UUID,
        billDate: LocalDate,
        billId: UUID?,
    ): List<EventForListDto> {
        val response =
            webClient
                .post()
                .uri("/wallet-entries/list")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "walletItemId" to walletItemId,
                        "billId" to billId,
                        "billDate" to billDate,
                        "pageRequest" to mapOf("size" to 20),
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        val body = requireNotNull(response).decodeToString()
        val itemsNode = objectMapper.readTree(body).get("items")
        return (0 until itemsNode.size())
            .map { index -> objectMapper.treeToValue(itemsNode.get(index), EventForListDto::class.java) }
            .sortedBy { it.date }
    }

    private fun editScheduledEntry(
        accessToken: String,
        recurrenceConfigId: UUID,
        request: EditScheduledEntryDto,
    ) {
        webClient
            .put()
            .uri("/wallet-entries/scheduled/$recurrenceConfigId")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isNoContent
    }

    private fun getScheduledEntry(
        accessToken: String,
        recurrenceConfigId: UUID,
    ): EventForListDto {
        val response =
            webClient
                .get()
                .uri("/wallet-entries/scheduled/$recurrenceConfigId")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        return objectMapper.readValue(requireNotNull(response), EventForListDto::class.java)
    }

    private fun getLatestScheduledEntry(accessToken: String): EventForListDto =
        listScheduled(accessToken, ScheduledExecutionFilter.FUTURE).first()

    private fun listScheduled(
        accessToken: String,
        filter: ScheduledExecutionFilter,
    ): List<EventForListDto> {
        val response =
            webClient
                .post()
                .uri("/wallet-entries/scheduled-executions/list")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    ScheduledExecutionManagerRequestDto(
                        groupId = null,
                        filter = filter,
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        val bytes = requireNotNull(response)
        val type = objectMapper.typeFactory.constructCollectionType(List::class.java, EventForListDto::class.java)
        return objectMapper.readValue(bytes, type)
    }

    private fun getCreditCard(
        accessToken: String,
        creditCardId: UUID,
    ): CreditCardDto {
        val response =
            webClient
                .get()
                .uri("/credit-cards/$creditCardId")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        return objectMapper.readValue(requireNotNull(response), CreditCardDto::class.java)
    }
}
