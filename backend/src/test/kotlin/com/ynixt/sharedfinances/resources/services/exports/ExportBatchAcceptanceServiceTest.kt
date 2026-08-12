package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.application.config.ExportRetentionProperties
import com.ynixt.sharedfinances.application.web.validation.ExportLineLimitValidator
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.entities.exports.ExportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ExportFormat
import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.ExportLineLimitExceededException
import com.ynixt.sharedfinances.domain.exceptions.http.ExportSelectionEmptyException
import com.ynixt.sharedfinances.domain.models.exports.ActiveRecurrenceExportRow
import com.ynixt.sharedfinances.domain.models.exports.CreateExport
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportFilter
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportRow
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.queue.producer.ExportJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.repositories.ExportBatchRepository
import com.ynixt.sharedfinances.domain.repositories.TransactionExportRepository
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExportBatchAcceptanceServiceTest {
    private val userId = UUID.randomUUID()
    private val request = CreateExport(ExportFormat.CSV, TransactionExportFilter())

    @Test
    fun `checks monthly quota before counting rows`() =
        runTest {
            val order = mutableListOf<String>()
            val quota = RecordingQuota(order, IllegalStateException("quota exhausted"))
            val rows = RecordingRows(order, 2)

            assertFailsWith<IllegalStateException> { service(quota, rows).accept(userId, UserPlanRole.USER, request) }

            assertEquals(listOf("quota"), order)
        }

    @Test
    fun `rejects an empty selection without saving or dispatching`() =
        runTest {
            val batch = RecordingBatches()
            val queue = RecordingQueue()

            assertFailsWith<ExportSelectionEmptyException> {
                service(RecordingQuota(), RecordingRows(count = 0), batch, queue).accept(userId, UserPlanRole.USER, request)
            }

            assertEquals(0, batch.saves)
            assertEquals(emptyList(), queue.ids)
        }

    @Test
    fun `rejects an over-cap selection after counting without consuming a batch`() =
        runTest {
            val batch = RecordingBatches()
            val queue = RecordingQueue()

            assertFailsWith<ExportLineLimitExceededException> {
                service(RecordingQuota(), RecordingRows(count = 2), batch, queue, maximum = 1)
                    .accept(userId, UserPlanRole.USER, request)
            }

            assertEquals(0, batch.saves)
            assertEquals(emptyList(), queue.ids)
        }

    @Test
    fun `accepted transfer-sized selection is queued but quota is not counted until completion`() =
        runTest {
            val order = mutableListOf<String>()
            val batch = RecordingBatches(order)
            val queue = RecordingQueue(order)

            val saved =
                service(RecordingQuota(order), RecordingRows(order, 2), batch, queue, maximum = 2)
                    .accept(userId, UserPlanRole.USER, request)

            assertEquals(2, saved.rowCount)
            assertNotNull(saved.createdAt)
            assertNull(saved.countedAt)
            assertEquals(listOf("quota", "count", "save", "dispatch"), order)
            assertEquals(listOf(saved.id), queue.ids)
        }

    private fun service(
        quota: RecordingQuota,
        rows: RecordingRows,
        batches: RecordingBatches = RecordingBatches(),
        queue: RecordingQueue = RecordingQueue(),
        maximum: Int? = 50_000,
    ) = ExportBatchAcceptanceService(
        quota,
        ExportLineLimitValidator(FixedPlanLimit(maximum)),
        rows,
        batches,
        queue,
        ExportBatchEventPublisher(RecordingEvents(), ExportRetentionProperties()),
        jacksonObjectMapper(),
        Clock.fixed(Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC),
    )

    private class RecordingQuota(
        private val order: MutableList<String> = mutableListOf(),
        private val failure: RuntimeException? = null,
    ) : PlanQuotaService {
        override suspend fun assertCanAdd(
            quotaOwnerUserId: UUID,
            quota: PlanLimitKey,
            requesterUserId: UUID,
        ) {
            order += "quota"
            failure?.let { throw it }
        }

        override suspend fun currentUsage(
            userId: UUID,
            quota: PlanLimitKey,
        ) = 0L

        override suspend fun usageChanged(
            userId: UUID,
            quota: PlanLimitKey,
        ) = Unit
    }

    private class RecordingRows(
        private val order: MutableList<String> = mutableListOf(),
        private val count: Long,
    ) : TransactionExportRepository {
        override suspend fun countLines(
            userId: UUID,
            filter: TransactionExportFilter,
        ): Long = count.also { order += "count" }

        override suspend fun findRows(
            userId: UUID,
            filter: TransactionExportFilter,
            pageSize: Int,
        ): Flow<TransactionExportRow> = emptyFlow()

        override suspend fun findActiveRecurrences(
            userId: UUID,
            filter: TransactionExportFilter,
        ): Flow<ActiveRecurrenceExportRow> = emptyFlow()
    }

    private class RecordingBatches(
        private val order: MutableList<String> = mutableListOf(),
    ) : ExportBatchRepository {
        var saves = 0

        override fun <S : ExportBatchEntity> save(entity: S): Mono<S> =
            Mono.just(
                entity.also {
                    it.id = UUID.randomUUID()
                    saves++
                    order +=
                        "save"
                },
            )

        override fun findAllByUserId(userId: UUID) = Flux.empty<ExportBatchEntity>()

        override fun findByIdAndUserId(
            id: UUID,
            userId: UUID,
        ) = Mono.empty<ExportBatchEntity>()

        override fun findById(id: UUID) = Mono.empty<ExportBatchEntity>()

        override fun deleteById(id: UUID) = Mono.just(0L)

        override fun existsById(id: UUID) = Mono.just(false)

        override fun <S : ExportBatchEntity> saveAll(entity: Iterable<S>) = Flux.fromIterable(entity)

        override fun findAllByIdIn(id: Collection<UUID>) = Flux.empty<ExportBatchEntity>()
    }

    private class RecordingQueue(
        private val order: MutableList<String> = mutableListOf(),
    ) : ExportJobDispatchQueueProducer {
        val ids = mutableListOf<UUID>()

        override fun send(batchId: UUID) {
            ids += batchId
            order += "dispatch"
        }
    }

    private class FixedPlanLimit(
        private val maximum: Int?,
    ) : PlanLimitService {
        override suspend fun resolve(
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = ResolvedPlanLimit(maximum)

        override suspend fun resolve(
            tier: GroupPlanTier,
            quota: PlanLimitKey,
        ) = ResolvedPlanLimit(maximum)

        override suspend fun save(limit: PlanLimitEntity) = limit

        override suspend fun delete(
            scope: PlanLimitScope,
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = Unit
    }

    private class RecordingEvents : ActionEventService {
        override fun getDestinationForUser(userId: UUID) = userId.toString()

        override fun getDestinationForGroup(userId: UUID) = userId.toString()

        override suspend fun <T> newEvent(
            userId: UUID,
            type: ActionEventType,
            category: ActionEventCategory,
            data: T,
            groupInfo: NewEventGroupInfo?,
        ) = Unit
    }
}
