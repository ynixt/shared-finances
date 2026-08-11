package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.models.imports.CreateImport
import com.ynixt.sharedfinances.domain.models.imports.ImportLine
import com.ynixt.sharedfinances.domain.queue.producer.ImportJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.domain.repositories.ImportDuplicateRepository
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ImportBatchDispatchRepository
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import com.ynixt.sharedfinances.scenarios.support.NoOpPlanQuotaService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ImportServiceImplTest {
    @Test
    fun `should keep the persisted batch queued when initial publication fails`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val walletItemId = UUID.randomUUID()
            val batchId = UUID.randomUUID()
            val request =
                CreateImport(
                    fileHash = "a".repeat(64),
                    fileName = "statement.csv",
                    lines =
                        listOf(
                            ImportLine(
                                walletItemId = walletItemId,
                                name = "Purchase",
                                value = BigDecimal("-10.00"),
                                date = LocalDate.of(2026, 8, 7),
                            ),
                        ),
                )
            val batch =
                ImportBatchEntity(
                    userId = userId,
                    fileHash = request.fileHash,
                    fileName = request.fileName,
                    format = "CSV",
                    walletItemId = walletItemId,
                    qty = 1,
                    totalCredit = BigDecimal.ZERO,
                    totalDebit = BigDecimal.TEN,
                    status = ImportBatchStatus.QUEUED,
                    requestPayload = "{}",
                    errorMessage = null,
                    leaseExpiresAt = null,
                    workerId = null,
                    startedAt = null,
                    finishedAt = null,
                    retries = 0,
                ).apply {
                    id = batchId
                    createdAt = OffsetDateTime.parse("2026-08-07T12:00:00Z")
                }
            val acceptanceService = Mockito.mock(ImportBatchAcceptanceService::class.java)
            Mockito
                .`when`(
                    acceptanceService
                        .accept(userId, request),
                ).thenReturn(AcceptedImportBatch(batch, "Checking", created = true))
            val producer =
                object : ImportJobDispatchQueueProducer {
                    override fun send(batchId: UUID) {
                        error("JetStream unavailable")
                    }
                }
            val service =
                ImportServiceImpl(
                    importBatchRepository = Mockito.mock(ImportBatchRepository::class.java),
                    duplicateRepository = Mockito.mock(ImportDuplicateRepository::class.java),
                    walletItemService = Mockito.mock(WalletItemService::class.java),
                    acceptanceService = acceptanceService,
                    dispatchRepository = Mockito.mock(ImportBatchDispatchRepository::class.java),
                    dispatchQueueProducer = producer,
                    eventPublisher = ImportBatchEventPublisher(NoOpActionEventService()),
                    planQuotaService = NoOpPlanQuotaService,
                )

            val accepted = service.create(userId, request)

            assertThat(accepted.id).isEqualTo(batchId)
            assertThat(accepted.status).isEqualTo(ImportBatchStatus.QUEUED)
            Unit
        }

    private class NoOpActionEventService : ActionEventService {
        override fun getDestinationForUser(userId: UUID): String = "user:$userId"

        override fun getDestinationForGroup(userId: UUID): String = "group:$userId"

        override suspend fun <T> newEvent(
            userId: UUID,
            type: ActionEventType,
            category: ActionEventCategory,
            data: T,
            groupInfo: NewEventGroupInfo?,
        ) = Unit
    }
}
