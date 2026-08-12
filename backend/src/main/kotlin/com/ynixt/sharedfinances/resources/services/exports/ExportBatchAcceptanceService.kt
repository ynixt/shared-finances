package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.application.web.validation.ExportLineLimitValidator
import com.ynixt.sharedfinances.domain.entities.exports.ExportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.ExportSelectionEmptyException
import com.ynixt.sharedfinances.domain.models.exports.CreateExport
import com.ynixt.sharedfinances.domain.queue.producer.ExportJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.repositories.ExportBatchRepository
import com.ynixt.sharedfinances.domain.repositories.TransactionExportRepository
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ExportBatchAcceptanceService(
    private val quotaService: PlanQuotaService,
    private val lineLimitValidator: ExportLineLimitValidator,
    private val transactionExportRepository: TransactionExportRepository,
    private val batchRepository: ExportBatchRepository,
    private val queueProducer: ExportJobDispatchQueueProducer,
    private val eventPublisher: ExportBatchEventPublisher,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    @Transactional
    suspend fun accept(
        userId: UUID,
        role: UserPlanRole,
        request: CreateExport,
    ): ExportBatchEntity {
        quotaService.assertCanAdd(userId, PlanLimitKey.EXPORTS_PER_MONTH)
        val count = transactionExportRepository.countLines(userId, request.filter)
        if (count == 0L) throw ExportSelectionEmptyException()
        lineLimitValidator.validate(role, count)
        val batch =
            ExportBatchEntity(
                userId = userId,
                status = ExportBatchStatus.QUEUED,
                format = request.format,
                filterPayload = objectMapper.writeValueAsString(request.filter),
                rowCount = count.toInt(),
                countedAt = null,
                startedAt = null,
                finishedAt = null,
                firstDownloadedAt = null,
                fileKey = null,
                fileDeletedAt = null,
                errorMessage = null,
                leaseExpiresAt = null,
                workerId = null,
                retries = 0,
            ).also { it.createdAt = OffsetDateTime.now(clock) }
        val saved =
            batchRepository
                .save(batch)
                .awaitSingle()
        eventPublisher.publish(saved, ActionEventType.INSERT)
        queueProducer.send(requireNotNull(saved.id))
        return saved
    }
}
