package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.application.config.ExportRetentionProperties
import com.ynixt.sharedfinances.domain.entities.exports.ExportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ExportFormat
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.exports.CreateExport
import com.ynixt.sharedfinances.domain.models.exports.ExportBatchSummary
import com.ynixt.sharedfinances.domain.repositories.ExportBatchRepository
import com.ynixt.sharedfinances.domain.services.FileStorageService
import com.ynixt.sharedfinances.domain.services.exports.ExportDownload
import com.ynixt.sharedfinances.domain.services.exports.ExportService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ExportBatchDispatchRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ExportServiceImpl(
    private val acceptanceService: ExportBatchAcceptanceService,
    private val batchRepository: ExportBatchRepository,
    private val dispatchRepository: ExportBatchDispatchRepository,
    private val storageService: FileStorageService,
    private val eventPublisher: ExportBatchEventPublisher,
    private val retentionProperties: ExportRetentionProperties,
    private val clock: Clock,
) : ExportService {
    override suspend fun create(
        userId: UUID,
        role: UserPlanRole,
        request: CreateExport,
    ): ExportBatchSummary = acceptanceService.accept(userId, role, request).toSummary()

    override suspend fun get(
        userId: UUID,
        batchId: UUID,
    ): ExportBatchSummary? = batchRepository.findByIdAndUserId(batchId, userId).awaitSingleOrNull()?.toSummary()

    override suspend fun list(userId: UUID): List<ExportBatchSummary> =
        batchRepository
            .findAllByUserId(userId)
            .asFlow()
            .toList()
            .filter { it.status != ExportBatchStatus.EXPIRED }
            .map { it.toSummary() }

    override suspend fun download(
        userId: UUID,
        batchId: UUID,
    ): ExportDownload? {
        val batch = batchRepository.findByIdAndUserId(batchId, userId).awaitSingleOrNull() ?: return null
        if (batch.status != ExportBatchStatus.COMPLETED) return null
        val key = batch.fileKey ?: return null
        val resource = storageService.find(key) ?: return null
        val firstDownloadedAt =
            dispatchRepository.recordFirstDownload(batchId, userId, OffsetDateTime.now(clock)).awaitSingleOrNull() ?: return null
        return ExportDownload(
            resource = resource,
            fileName = "transactions-$batchId.${batch.format.name.lowercase()}",
            contentType =
                if (batch.format == ExportFormat.CSV) {
                    "text/csv;charset=UTF-8"
                } else {
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                },
            firstDownloadedAt = firstDownloadedAt,
            downloadExpiresAt = firstDownloadedAt.plus(retentionProperties.afterDownload.delay),
        )
    }

    override suspend fun delete(
        userId: UUID,
        batchId: UUID,
    ): Boolean {
        val batch = batchRepository.findByIdAndUserId(batchId, userId).awaitSingleOrNull() ?: return false
        if (batch.status != ExportBatchStatus.COMPLETED) return false
        val fileRemoved = batch.fileKey?.let { storageService.delete(it) || storageService.find(it) == null } ?: true
        if (!fileRemoved) return false
        val deleted = dispatchRepository.deleteCompleted(batchId, userId).awaitSingle() == 1L
        if (deleted) eventPublisher.publishDeleted(userId, batchId)
        return deleted
    }

    private fun ExportBatchEntity.toSummary() =
        ExportBatchSummary(
            id = requireNotNull(id),
            format = format,
            status = status,
            rowCount = rowCount,
            errorMessage = errorMessage,
            createdAt = requireNotNull(createdAt),
            startedAt = startedAt,
            finishedAt = finishedAt,
            firstDownloadedAt = firstDownloadedAt,
            downloadExpiresAt = firstDownloadedAt?.plus(retentionProperties.afterDownload.delay),
            fileDeletedAt = fileDeletedAt,
            downloadAvailable = status == ExportBatchStatus.COMPLETED && fileKey != null && fileDeletedAt == null,
        )
}
