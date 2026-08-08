package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.imports.CreateImportDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportBatchDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportDuplicateCheckDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportDuplicateLineDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportHashCheckDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportLineDto
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ImportHashStatus
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.imports.ImportJobService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryRemovalService
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.config.TestClockConfig
import com.ynixt.sharedfinances.support.util.JsonUtil
import com.ynixt.sharedfinances.support.util.MutableTestClock
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestClockConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ImportControllerIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: com.ynixt.sharedfinances.domain.repositories.UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var walletEventRepository: WalletEventRepository

    @Autowired
    private lateinit var recurrenceEventRepository: RecurrenceEventRepository

    @Autowired
    private lateinit var importBatchRepository: ImportBatchRepository

    @Autowired
    private lateinit var importJobService: ImportJobService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var mutableTestClock: MutableTestClock

    @MockitoSpyBean
    private lateinit var walletEntryRemovalService: WalletEntryRemovalService

    private lateinit var userTestUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        mutableTestClock.setDate(LocalDate.of(2026, 8, 10))
        userTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
    }

    @Test
    fun `should create every installment segment shape and undo it releasing balances and hash`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val bankAccountJson = JsonUtil.readJsonFromResources("mocks/bank-account/new-bank-account-request-200.json")
            webClient
                .post()
                .uri("/bank-accounts")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bankAccountJson)
                .exchange()
                .expectStatus()
                .isOk

            val walletItemId =
                walletItemRepository
                    .findAllByUserIdAndType(
                        userId = requireNotNull(user.id),
                        type = WalletItemType.BANK_ACCOUNT,
                        pageable = PageRequest.of(0, 1),
                    ).collectList()
                    .awaitSingle()
                    .single()
                    .id!!

            data class Case(
                val previous: Boolean,
                val following: Boolean,
                val expectedQty: Int,
                val expectedPostedEvents: Int,
                val expectedRecurrenceConfigs: Int,
            )

            val cases =
                listOf(
                    Case(previous = false, following = false, expectedQty = 1, expectedPostedEvents = 1, expectedRecurrenceConfigs = 1),
                    Case(previous = true, following = false, expectedQty = 4, expectedPostedEvents = 4, expectedRecurrenceConfigs = 2),
                    Case(previous = false, following = true, expectedQty = 3, expectedPostedEvents = 1, expectedRecurrenceConfigs = 1),
                    Case(previous = true, following = true, expectedQty = 6, expectedPostedEvents = 4, expectedRecurrenceConfigs = 2),
                )

            cases.forEachIndexed { index, case ->
                val hash = index.toString(16).padStart(64, '0')
                val createRequest =
                    CreateImportDto(
                        fileHash = hash,
                        fileName = "segment-$index.csv",
                        lines =
                            listOf(
                                ImportLineDto(
                                    walletItemId = walletItemId,
                                    name = "Installment 4/6",
                                    value = BigDecimal("-100.00"),
                                    date = LocalDate.of(2026, 8, 10),
                                    categoryId = null,
                                    groupId = null,
                                    beneficiaries = null,
                                    billDate = null,
                                    installment = 4,
                                    installmentTotal = 6,
                                    createPreviousInstallments = case.previous,
                                    createFollowingInstallments = case.following,
                                    tags = listOf("import-test"),
                                    observations = null,
                                ),
                            ),
                    )
                val created =
                    webClient
                        .post()
                        .uri("/imports")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(createRequest)
                        .exchange()
                        .expectStatus()
                        .isAccepted
                        .expectBody(ImportBatchDto::class.java)
                        .returnResult()
                        .responseBody!!

                assertThat(created.status).isEqualTo(ImportBatchStatus.QUEUED)
                val completed = awaitTerminalBatch(accessToken, created.id)
                assertThat(completed.status).isEqualTo(ImportBatchStatus.COMPLETED)
                assertThat(completed.qty).isEqualTo(case.expectedQty)
                assertThat(walletEventRepository.findAllByImportBatchId(created.id).asFlow().toList())
                    .hasSize(case.expectedPostedEvents)
                assertThat(recurrenceEventRepository.findAllByImportBatchId(created.id).asFlow().toList())
                    .hasSize(case.expectedRecurrenceConfigs)
                val beforeRedelivery = walletEventRepository.findAllByImportBatchId(created.id).asFlow().toList()
                importJobService.processDispatchMessage(created.id)
                val afterRedelivery = walletEventRepository.findAllByImportBatchId(created.id).asFlow().toList()
                assertThat(afterRedelivery.map { it.id })
                    .containsExactlyElementsOf(beforeRedelivery.map { it.id })
                assertThat(
                    importBatchRepository
                        .findFirstByUserIdAndFileHashAndStatusIn(user.id!!, hash, setOf(ImportBatchStatus.COMPLETED))
                        .awaitSingleOrNull(),
                ).isNotNull()

                val undoAccepted =
                    webClient
                        .delete()
                        .uri("/imports/${created.id}")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .exchange()
                        .expectStatus()
                        .isAccepted
                        .expectBody(ImportBatchDto::class.java)
                        .returnResult()
                        .responseBody!!
                assertThat(undoAccepted.status).isEqualTo(ImportBatchStatus.UNDO_QUEUED)
                awaitBatchRemoved(created.id)

                assertThat(walletEventRepository.findAllByImportBatchId(created.id).asFlow().toList()).isEmpty()
                assertThat(recurrenceEventRepository.findAllByImportBatchId(created.id).asFlow().toList()).isEmpty()
                assertThat(walletItemRepository.findOneById(walletItemId).awaitSingle().balance)
                    .isEqualByComparingTo(BigDecimal.ZERO)

                val hashCheck =
                    webClient
                        .get()
                        .uri("/imports/check-hash/$hash")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .exchange()
                        .expectStatus()
                        .isOk
                        .expectBody(ImportHashCheckDto::class.java)
                        .returnResult()
                        .responseBody!!
                assertThat(hashCheck.status).isEqualTo(ImportHashStatus.NOT_IMPORTED)

                if (index == cases.lastIndex) {
                    val reimported =
                        webClient
                            .post()
                            .uri("/imports")
                            .header(HttpHeaders.AUTHORIZATION, accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createRequest)
                            .exchange()
                            .expectStatus()
                            .isAccepted
                            .expectBody(ImportBatchDto::class.java)
                            .returnResult()
                            .responseBody!!

                    awaitTerminalBatch(accessToken, reimported.id)
                    webClient
                        .delete()
                        .uri("/imports/${reimported.id}")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .exchange()
                        .expectStatus()
                        .isAccepted
                    awaitBatchRemoved(reimported.id)
                }
            }
        }

    @Test
    fun `should rollback every financial row and fail safely after retry limit`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val walletItemId = createBankAccount(accessToken, user.id!!)
            val created =
                webClient
                    .post()
                    .uri("/imports")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        CreateImportDto(
                            fileHash = "f".repeat(64),
                            fileName = "rollback.csv",
                            lines =
                                listOf(
                                    importLine(walletItemId, "Valid first row", BigDecimal("-10.00")),
                                    importLine(walletItemId, "Invalid second row", BigDecimal("-20.00")).copy(
                                        categoryId = java.util.UUID.randomUUID(),
                                    ),
                                ),
                        ),
                    ).exchange()
                    .expectStatus()
                    .isAccepted
                    .expectBody(ImportBatchDto::class.java)
                    .returnResult()
                    .responseBody!!

            val failed = awaitTerminalBatch(accessToken, created.id)
            assertThat(failed.status).isEqualTo(ImportBatchStatus.FAILED)
            assertThat(failed.retries).isEqualTo(3)
            assertThat(failed.errorMessage).doesNotContain("Exception", "SQL", "categoryId")
            assertThat(walletEventRepository.findAllByImportBatchId(created.id).asFlow().toList()).isEmpty()
            assertThat(recurrenceEventRepository.findAllByImportBatchId(created.id).asFlow().toList()).isEmpty()
            assertThat(walletItemRepository.findOneById(walletItemId).awaitSingle().balance).isEqualByComparingTo(BigDecimal.ZERO)
            Unit
        }

    @Test
    fun `should rollback partial undo work and preserve the import when undo retries fail`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val walletItemId = createBankAccount(accessToken, user.id!!)
            val created =
                webClient
                    .post()
                    .uri("/imports")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        CreateImportDto(
                            fileHash = "e".repeat(64),
                            fileName = "undo-rollback.csv",
                            lines =
                                listOf(
                                    importLine(walletItemId, "Installments", BigDecimal("-10.00")).copy(
                                        installment = 1,
                                        installmentTotal = 2,
                                        createFollowingInstallments = true,
                                    ),
                                    importLine(walletItemId, "One off", BigDecimal("-20.00")),
                                ),
                        ),
                    ).exchange()
                    .expectStatus()
                    .isAccepted
                    .expectBody(ImportBatchDto::class.java)
                    .returnResult()
                    .responseBody!!

            val completed = awaitTerminalBatch(accessToken, created.id)
            assertThat(completed.status).isEqualTo(ImportBatchStatus.COMPLETED)
            val walletEventIdsBefore =
                walletEventRepository
                    .findAllByImportBatchId(created.id)
                    .asFlow()
                    .toList()
                    .map { it.id }
            val recurrenceIdsBefore =
                recurrenceEventRepository
                    .findAllByImportBatchId(created.id)
                    .asFlow()
                    .toList()
                    .map { it.id }
            val balanceBefore = walletItemRepository.findOneById(walletItemId).awaitSingle().balance

            Mockito
                .doThrow(IllegalStateException("forced undo failure"))
                .`when`(walletEntryRemovalService)
                .deleteOneOff(Mockito.any(java.util.UUID::class.java), Mockito.any(java.util.UUID::class.java))

            webClient
                .delete()
                .uri("/imports/${created.id}")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .exchange()
                .expectStatus()
                .isAccepted

            val failed = awaitUndoFailedBatch(accessToken, created.id)
            assertThat(failed.retries).isEqualTo(3)
            assertThat(
                walletEventRepository
                    .findAllByImportBatchId(created.id)
                    .asFlow()
                    .toList()
                    .map { it.id },
            ).containsExactlyInAnyOrderElementsOf(walletEventIdsBefore)
            assertThat(
                recurrenceEventRepository
                    .findAllByImportBatchId(created.id)
                    .asFlow()
                    .toList()
                    .map { it.id },
            ).containsExactlyInAnyOrderElementsOf(recurrenceIdsBefore)
            assertThat(walletItemRepository.findOneById(walletItemId).awaitSingle().balance)
                .isEqualByComparingTo(balanceBefore)
        }

    @Test
    fun `should create one batch with origins resolved per line`() =
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val firstRequest = JsonUtil.readJsonFromResources("mocks/bank-account/new-bank-account-request-200.json")
            val secondRequest = firstRequest.replace("Conta IT", "Conta IT 2")

            listOf(firstRequest, secondRequest).forEach { request ->
                webClient
                    .post()
                    .uri("/bank-accounts")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus()
                    .isOk
            }

            val walletItems =
                walletItemRepository
                    .findAllByUserIdAndType(
                        userId = user.id!!,
                        type = WalletItemType.BANK_ACCOUNT,
                        pageable = PageRequest.of(0, 10),
                    ).collectList()
                    .awaitSingle()
                    .associateBy { it.name }

            val firstWalletItemId = walletItems.getValue("Conta IT").id!!
            val secondWalletItemId = walletItems.getValue("Conta IT 2").id!!
            val created =
                webClient
                    .post()
                    .uri("/imports")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        CreateImportDto(
                            fileHash = "a".repeat(64),
                            fileName = "multiple-origins.csv",
                            lines =
                                listOf(
                                    importLine(firstWalletItemId, "Primeira origem", BigDecimal("-10.00")),
                                    importLine(secondWalletItemId, "Segunda origem", BigDecimal("-20.00")),
                                ),
                        ),
                    ).exchange()
                    .expectStatus()
                    .isAccepted
                    .expectBody(ImportBatchDto::class.java)
                    .returnResult()
                    .responseBody!!

            val completed = awaitTerminalBatch(accessToken, created.id)
            assertThat(completed.walletItemId).isNull()
            assertThat(completed.walletItemName).isEqualTo("Múltiplas origens")
            assertThat(walletItemRepository.findOneById(firstWalletItemId).awaitSingle().balance)
                .isEqualByComparingTo(BigDecimal("-10.00"))
            assertThat(walletItemRepository.findOneById(secondWalletItemId).awaitSingle().balance)
                .isEqualByComparingTo(BigDecimal("-20.00"))

            webClient
                .post()
                .uri("/imports/check-duplicates")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    ImportDuplicateCheckDto(
                        lines =
                            listOf(
                                duplicateLine(firstWalletItemId, "Primeira origem", BigDecimal("-10.00")),
                                duplicateLine(secondWalletItemId, "Segunda origem", BigDecimal("-20.00")),
                            ),
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBodyList(Int::class.java)
                .hasSize(2)
                .contains(0, 1)
            Unit
        }

    private fun importLine(
        walletItemId: java.util.UUID,
        name: String,
        value: BigDecimal,
    ) = ImportLineDto(
        walletItemId = walletItemId,
        name = name,
        value = value,
        date = LocalDate.of(2026, 8, 10),
        categoryId = null,
        groupId = null,
        beneficiaries = null,
        billDate = null,
        installment = null,
        installmentTotal = null,
        tags = null,
        observations = null,
    )

    private fun duplicateLine(
        walletItemId: java.util.UUID,
        name: String,
        value: BigDecimal,
    ) = ImportDuplicateLineDto(
        walletItemId = walletItemId,
        name = name,
        value = value,
        date = LocalDate.of(2026, 8, 10),
        installment = null,
    )

    private suspend fun createBankAccount(
        accessToken: String,
        userId: java.util.UUID,
    ): java.util.UUID {
        val bankAccountJson = JsonUtil.readJsonFromResources("mocks/bank-account/new-bank-account-request-200.json")
        webClient
            .post()
            .uri("/bank-accounts")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(bankAccountJson)
            .exchange()
            .expectStatus()
            .isOk
        return walletItemRepository
            .findAllByUserIdAndType(userId, WalletItemType.BANK_ACCOUNT, PageRequest.of(0, 1))
            .collectList()
            .awaitSingle()
            .single()
            .id!!
    }

    private suspend fun awaitTerminalBatch(
        accessToken: String,
        batchId: java.util.UUID,
    ): ImportBatchDto {
        repeat(100) {
            val batch =
                webClient
                    .get()
                    .uri("/imports/$batchId")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(ImportBatchDto::class.java)
                    .returnResult()
                    .responseBody!!
            if (batch.status == ImportBatchStatus.COMPLETED || batch.status == ImportBatchStatus.FAILED) {
                return batch
            }
            delay(100)
        }
        error("Import batch $batchId did not reach a terminal state")
    }

    private suspend fun awaitBatchRemoved(batchId: java.util.UUID) {
        repeat(100) {
            if (importBatchRepository.findById(batchId).awaitSingleOrNull() == null) return
            delay(100)
        }
        error("Import batch $batchId was not removed after undo")
    }

    private suspend fun awaitUndoFailedBatch(
        accessToken: String,
        batchId: java.util.UUID,
    ): ImportBatchDto {
        repeat(100) {
            val batch =
                webClient
                    .get()
                    .uri("/imports/$batchId")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(ImportBatchDto::class.java)
                    .returnResult()
                    .responseBody!!
            if (batch.status == ImportBatchStatus.UNDO_FAILED) return batch
            delay(100)
        }
        error("Import batch $batchId did not reach UNDO_FAILED")
    }
}
