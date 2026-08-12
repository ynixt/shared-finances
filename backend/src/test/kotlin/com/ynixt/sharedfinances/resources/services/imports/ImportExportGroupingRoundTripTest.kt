package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.imports.CreateImport
import com.ynixt.sharedfinances.domain.models.imports.ImportLine
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ImportBatchDispatchRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImportExportGroupingRoundTripTest {
    private val mapper = jacksonObjectMapper()
    private val userId = UUID.randomUUID()
    private val originId = UUID.randomUUID()
    private val targetId = UUID.randomUUID()
    private val seriesWalletId = UUID.randomUUID()
    private val awkwardText = "market; \"weekly\"\nrun"

    @Test
    fun `canonical transfer and installments reconstruct one event and one shared series without changing text`() =
        runTest {
            val creator = RecordingCreator(userId)
            execute(creator, fixture())

            val transfer = creator.requests.single { it.type == WalletEntryType.TRANSFER }
            assertEquals(originId, transfer.originId)
            assertEquals(targetId, transfer.targetId)
            assertEquals(BigDecimal("42.30"), transfer.originValue)
            assertEquals(BigDecimal("42.30"), transfer.targetValue)
            assertEquals(awkwardText, transfer.name)
            assertEquals("first line\nsecond; \"quoted\" line", transfer.observations)

            val installments = creator.requests.filter { it.paymentType == PaymentType.INSTALLMENTS }
            assertEquals(3, installments.size)
            assertNull(installments.first().seriesId)
            assertEquals(listOf(0, 1, 2), installments.map { it.seriesOffset })
            assertEquals(listOf("installment-1", "installment-2", "installment-3"), installments.map { it.externalTransactionId })
            assertEquals(creator.generatedSeriesId, installments[1].seriesId)
            assertEquals(creator.generatedSeriesId, installments[2].seriesId)
        }

    @Test
    fun `external transaction id remains stable across a second import cycle`() =
        runTest {
            val firstCreator = RecordingCreator(userId)
            execute(firstCreator, fixture())
            val exportedIds = firstCreator.requests.mapNotNull { it.externalTransactionId }

            val secondCreator = RecordingCreator(userId)
            execute(
                secondCreator,
                fixture().map { line ->
                    if (line.transferGroupId == null) line else line.copy(externalTransactionId = exportedIds.first())
                },
            )

            assertEquals(exportedIds, secondCreator.requests.mapNotNull { it.externalTransactionId })
        }

    private suspend fun execute(
        creator: RecordingCreator,
        lines: List<ImportLine>,
    ) {
        val batchId = UUID.randomUUID()
        val workerId = "worker-1"
        val batch =
            ImportBatchEntity(
                userId,
                "hash",
                "round-trip.csv",
                "CSV",
                null,
                lines.size,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                ImportBatchStatus.RUNNING,
                mapper.writeValueAsString(CreateImport("hash", "round-trip.csv", lines = lines)),
                null,
                null,
                workerId,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0,
            ).also { it.id = batchId }
        ImportBatchExecutionService(
            FixedImportBatchRepository(batch),
            SuccessfulDispatchRepository(),
            creator,
            mapper,
            Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC),
        ).execute(batchId, workerId)
    }

    private fun fixture(): List<ImportLine> =
        listOf(
            line(originId, "-42.30", "transfer-source", transferGroupId = "transfer-1", name = awkwardText),
            line(targetId, "42.30", "transfer-target", transferGroupId = "transfer-1", name = "target"),
            line(seriesWalletId, "10.00", "installment-1", seriesGroupId = "series-1", installment = 1),
            line(seriesWalletId, "10.00", "installment-2", seriesGroupId = "series-1", installment = 2),
            line(seriesWalletId, "10.00", "installment-3", seriesGroupId = "series-1", installment = 3),
        )

    private fun line(
        walletItemId: UUID,
        value: String,
        externalId: String,
        transferGroupId: String? = null,
        seriesGroupId: String? = null,
        installment: Int? = null,
        name: String = "installment",
    ) = ImportLine(
        walletItemId = walletItemId,
        name = name,
        value = BigDecimal(value),
        date = LocalDate.of(2026, 8, installment ?: 11),
        installment = installment,
        installmentTotal = installment?.let { 3 },
        observations = if (transferGroupId != null && value.startsWith('-')) "first line\nsecond; \"quoted\" line" else null,
        externalTransactionId = externalId,
        transferGroupId = transferGroupId,
        seriesGroupId = seriesGroupId,
    )

    private class RecordingCreator(
        private val userId: UUID,
    ) : WalletEntryCreateService {
        val requests = mutableListOf<NewEntryRequest>()
        val generatedSeriesId = UUID.randomUUID()

        override suspend fun create(
            userId: UUID,
            newEntryRequest: NewEntryRequest,
        ): WalletEventEntity {
            requests += newEntryRequest
            val event =
                WalletEventEntity(
                    newEntryRequest.type,
                    newEntryRequest.name,
                    newEntryRequest.categoryId,
                    userId,
                    newEntryRequest.groupId,
                    newEntryRequest.tags,
                    newEntryRequest.observations,
                    newEntryRequest.date,
                    newEntryRequest.confirmed,
                    newEntryRequest.seriesOffset.takeIf { newEntryRequest.paymentType == PaymentType.INSTALLMENTS }?.plus(1),
                    null,
                    newEntryRequest.importBatchId,
                    newEntryRequest.externalTransactionId,
                    newEntryRequest.paymentType,
                )
            if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) {
                event.recurrenceEvent =
                    RecurrenceEventEntity(
                        newEntryRequest.name,
                        newEntryRequest.categoryId,
                        this.userId,
                        newEntryRequest.groupId,
                        newEntryRequest.tags,
                        newEntryRequest.observations,
                        newEntryRequest.type,
                        RecurrenceType.MONTHLY,
                        PaymentType.INSTALLMENTS,
                        1,
                        newEntryRequest.seriesQtyTotal,
                        newEntryRequest.date,
                        null,
                        null,
                        newEntryRequest.seriesId ?: generatedSeriesId,
                        newEntryRequest.seriesOffset,
                    )
            }
            return event
        }

        override suspend fun createFromRecurrenceConfig(
            recurrenceConfigId: UUID,
            date: LocalDate,
            confirmedOverride: Boolean?,
        ) = null
    }

    private class FixedImportBatchRepository(
        private val batch: ImportBatchEntity,
    ) : ImportBatchRepository {
        override fun findById(id: UUID) = Mono.just(batch)

        override fun findFirstByUserIdAndFileHashAndStatusIn(
            userId: UUID,
            fileHash: String,
            statuses: Collection<ImportBatchStatus>,
        ) = Mono.empty<ImportBatchEntity>()

        override fun findAllByUserId(userId: UUID) = Flux.empty<ImportBatchEntity>()

        override fun findByIdAndUserId(
            id: UUID,
            userId: UUID,
        ) = Mono.empty<ImportBatchEntity>()

        override fun deleteById(id: UUID) = Mono.just(0L)

        override fun existsById(id: UUID) = Mono.just(false)

        override fun <S : ImportBatchEntity> save(entity: S) = Mono.just(entity)

        override fun <S : ImportBatchEntity> saveAll(entity: Iterable<S>) = Flux.fromIterable(entity)

        override fun findAllByIdIn(id: Collection<UUID>) = Flux.empty<ImportBatchEntity>()
    }

    private class SuccessfulDispatchRepository : ImportBatchDispatchRepository {
        override fun markCompleted(
            batchId: UUID,
            workerId: String,
            finishedAt: OffsetDateTime,
        ) = Mono.just(1L)

        override fun queueUndo(
            batchId: UUID,
            userId: UUID,
        ) = Mono.empty<UUID>()

        override fun claimOldestQueuedForUser(
            userId: UUID,
            workerId: String,
            now: OffsetDateTime,
            leaseExpiresAt: OffsetDateTime,
        ) = Mono.empty<UUID>()

        override fun renewLease(
            batchId: UUID,
            workerId: String,
            leaseExpiresAt: OffsetDateTime,
        ) = Mono.just(0L)

        override fun deleteClaimedUndoBatch(
            batchId: UUID,
            workerId: String,
        ) = Mono.just(0L)

        override fun markQueuedForRetry(
            batchId: UUID,
            workerId: String,
            errorMessage: String,
        ) = Mono.just(0L)

        override fun markFailed(
            batchId: UUID,
            workerId: String,
            errorMessage: String,
            finishedAt: OffsetDateTime,
        ) = Mono.just(0L)

        override fun recoverExpiredLeases(
            now: OffsetDateTime,
            maxRetries: Int,
        ) = Flux.empty<UUID>()

        override fun findUsersReadyForDispatch(limit: Int) = Flux.empty<UUID>()

        override fun findOldestQueuedBatchId(userId: UUID) = Mono.empty<UUID>()
    }
}
