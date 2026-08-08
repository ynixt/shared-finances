package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.imports.CreateImportDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportBatchDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportHashCheckDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportLineDto
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ImportHashStatus
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ImportBatchDispatchRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.JsonUtil
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["app.imports.worker.enabled=false", "app.jobs.imports.reconcile.cron-enabled=false"],
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ImportAsyncAcceptanceIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: com.ynixt.sharedfinances.domain.repositories.UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var walletEventRepository: WalletEventRepository

    @Autowired
    private lateinit var dispatchRepository: ImportBatchDispatchRepository

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `should accept quickly enforce active idempotency and expose lifecycle semantics`() =
        runBlocking {
            val ownerUtil =
                UserTestUtil(
                    webClient = webClient,
                    passwordEncoder = passwordEncoder,
                    userRepository = userRepository,
                )
            val owner = ownerUtil.createUserOnDatabase()
            val ownerToken = ownerUtil.login()
            val otherUtil =
                UserTestUtil(
                    webClient = webClient,
                    passwordEncoder = passwordEncoder,
                    userRepository = userRepository,
                )
            otherUtil.createUserOnDatabase()
            val otherToken = otherUtil.login()
            val walletItemId = createBankAccount(ownerToken, requireNotNull(owner.id))
            val request = largeRequest(walletItemId, "a".repeat(64))

            val first = create(ownerToken, request)
            assertThat(first.status).isEqualTo(ImportBatchStatus.QUEUED)
            assertThat(walletEventRepository.findAllByImportBatchId(first.id).asFlow().toList()).isEmpty()

            val duplicate = create(ownerToken, request)
            assertThat(duplicate.id).isEqualTo(first.id)

            webClient
                .get()
                .uri("/imports/${first.id}")
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .exchange()
                .expectStatus()
                .isOk
            webClient
                .get()
                .uri("/imports/${first.id}")
                .header(HttpHeaders.AUTHORIZATION, otherToken)
                .exchange()
                .expectStatus()
                .isNotFound
            webClient
                .delete()
                .uri("/imports/${first.id}")
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .exchange()
                .expectStatus()
                .isEqualTo(409)

            val processing = checkHash(ownerToken, request.fileHash)
            assertThat(processing.status).isEqualTo(ImportHashStatus.PROCESSING)
            assertThat(processing.batchId).isEqualTo(first.id)

            setTerminal(first.id, ImportBatchStatus.FAILED)
            assertThat(checkHash(ownerToken, request.fileHash).status).isEqualTo(ImportHashStatus.NOT_IMPORTED)
            val afterFailure = create(ownerToken, request)
            assertThat(afterFailure.id).isNotEqualTo(first.id)

            setTerminal(afterFailure.id, ImportBatchStatus.COMPLETED)
            assertThat(checkHash(ownerToken, request.fileHash).status).isEqualTo(ImportHashStatus.IMPORTED)
            val afterCompletion = create(ownerToken, request)
            assertThat(afterCompletion.id).isNotEqualTo(afterFailure.id)

            val another = create(ownerToken, largeRequest(walletItemId, "b".repeat(64)))
            val workerId = "integration-worker"
            val now = OffsetDateTime.now()
            val claims =
                coroutineScope {
                    listOf(
                        async {
                            dispatchRepository
                                .claimOldestQueuedForUser(owner.id!!, "$workerId-1", now, now.plusMinutes(1))
                                .awaitSingleOrNull()
                        },
                        async {
                            dispatchRepository
                                .claimOldestQueuedForUser(owner.id!!, "$workerId-2", now, now.plusMinutes(1))
                                .awaitSingleOrNull()
                        },
                    ).map { it.await() }
                }
            assertThat(claims.filterNotNull()).hasSize(1)
            val claimedId = claims.filterNotNull().single()
            val claimedWorker = if (claims[0] != null) "$workerId-1" else "$workerId-2"
            assertThat(dispatchRepository.renewLease(claimedId, "wrong-worker", now.plusMinutes(1)).awaitSingle()).isZero()
            assertThat(dispatchRepository.renewLease(claimedId, claimedWorker, now.plusMinutes(1)).awaitSingle()).isOne()
            assertThat(dispatchRepository.markQueuedForRetry(claimedId, claimedWorker, "retry").awaitSingle()).isOne()
            assertThat(
                dispatchRepository
                    .claimOldestQueuedForUser(owner.id!!, workerId, now, now.plusMinutes(1))
                    .awaitSingle(),
            ).isEqualTo(claimedId)
            assertThat(dispatchRepository.renewLease(claimedId, workerId, now.minusSeconds(1)).awaitSingle()).isOne()
            assertThat(dispatchRepository.recoverExpiredLeases(now, 3).collectList().awaitSingle()).contains(claimedId)
            assertThat(
                dispatchRepository
                    .claimOldestQueuedForUser(owner.id!!, workerId, now, now.plusMinutes(1))
                    .awaitSingle(),
            ).isEqualTo(claimedId)
            assertThat(dispatchRepository.markFailed(claimedId, workerId, "failed", now).awaitSingle()).isOne()
            assertThat(another.id).isNotNull()
            Unit
        }

    @Test
    fun `should accept undo idempotently and apply undo claim retry and failure lifecycle`() =
        runBlocking {
            val userUtil =
                UserTestUtil(
                    webClient = webClient,
                    passwordEncoder = passwordEncoder,
                    userRepository = userRepository,
                )
            val user = userUtil.createUserOnDatabase()
            val token = userUtil.login()
            val walletItemId = createBankAccount(token, requireNotNull(user.id))
            val created = create(token, largeRequest(walletItemId, "c".repeat(64)))
            setTerminal(created.id, ImportBatchStatus.COMPLETED)

            val firstAccepted = acceptUndo(token, created.id)
            val duplicateAccepted = acceptUndo(token, created.id)
            assertThat(firstAccepted.status).isEqualTo(ImportBatchStatus.UNDO_QUEUED)
            assertThat(duplicateAccepted.id).isEqualTo(firstAccepted.id)
            assertThat(duplicateAccepted.status).isEqualTo(ImportBatchStatus.UNDO_QUEUED)
            assertThat(checkHash(token, created.fileHash).status).isEqualTo(ImportHashStatus.PROCESSING)

            val now = OffsetDateTime.now()
            assertThat(
                dispatchRepository
                    .claimOldestQueuedForUser(user.id!!, "undo-worker", now, now.plusMinutes(1))
                    .awaitSingle(),
            ).isEqualTo(created.id)
            assertThat(dispatchRepository.markQueuedForRetry(created.id, "undo-worker", "retry").awaitSingle()).isOne()
            assertThat(
                dispatchRepository
                    .claimOldestQueuedForUser(user.id!!, "undo-worker-2", now, now.plusMinutes(1))
                    .awaitSingle(),
            ).isEqualTo(created.id)
            assertThat(dispatchRepository.markFailed(created.id, "undo-worker-2", "failed", now).awaitSingle()).isOne()
            assertThat(get(token, created.id).status).isEqualTo(ImportBatchStatus.UNDO_FAILED)
            assertThat(checkHash(token, created.fileHash).status).isEqualTo(ImportHashStatus.IMPORTED)
        }

    private suspend fun createBankAccount(
        accessToken: String,
        userId: UUID,
    ): UUID {
        webClient
            .post()
            .uri("/bank-accounts")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(JsonUtil.readJsonFromResources("mocks/bank-account/new-bank-account-request-200.json"))
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

    private fun largeRequest(
        walletItemId: UUID,
        hash: String,
    ) = CreateImportDto(
        fileHash = hash,
        fileName = "large.csv",
        lines =
            (1..300).map { index ->
                ImportLineDto(
                    walletItemId = walletItemId,
                    name = "Line $index",
                    value = BigDecimal("-1.00"),
                    date = java.time.LocalDate.of(2026, 8, 7),
                    categoryId = null,
                    groupId = null,
                    beneficiaries = null,
                    billDate = null,
                    installment = null,
                    installmentTotal = null,
                    tags = null,
                    observations = null,
                )
            },
    )

    private fun create(
        token: String,
        request: CreateImportDto,
    ): ImportBatchDto =
        webClient
            .post()
            .uri("/imports")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody(ImportBatchDto::class.java)
            .returnResult()
            .responseBody!!

    private fun acceptUndo(
        token: String,
        batchId: UUID,
    ): ImportBatchDto =
        webClient
            .delete()
            .uri("/imports/$batchId")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody(ImportBatchDto::class.java)
            .returnResult()
            .responseBody!!

    private fun get(
        token: String,
        batchId: UUID,
    ): ImportBatchDto =
        webClient
            .get()
            .uri("/imports/$batchId")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ImportBatchDto::class.java)
            .returnResult()
            .responseBody!!

    private fun checkHash(
        token: String,
        hash: String,
    ): ImportHashCheckDto =
        webClient
            .get()
            .uri("/imports/check-hash/$hash")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ImportHashCheckDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun setTerminal(
        batchId: UUID,
        status: ImportBatchStatus,
    ) {
        databaseClient
            .sql(
                """
                UPDATE import_batch
                SET status = :status, request_payload = NULL, finished_at = NOW(), updated_at = NOW()
                WHERE id = :batchId
                """,
            ).bind("status", status.name)
            .bind("batchId", batchId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }
}
