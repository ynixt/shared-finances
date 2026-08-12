package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.application.config.ExportRetentionProperties
import com.ynixt.sharedfinances.application.web.dto.exports.ExportBatchStatusEventDto
import com.ynixt.sharedfinances.domain.entities.exports.ExportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExportBatchEventPublisher(
    private val actionEventService: ActionEventService,
    private val retentionProperties: ExportRetentionProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun publish(
        batch: ExportBatchEntity,
        type: ActionEventType,
    ) {
        val id = requireNotNull(batch.id)
        runCatching {
            actionEventService.newEvent(
                userId = batch.userId,
                type = type,
                category = ActionEventCategory.EXPORT_BATCH,
                data =
                    ExportBatchStatusEventDto(
                        id = id,
                        format = batch.format,
                        status = batch.status,
                        errorMessage = batch.errorMessage,
                        rowCount = batch.rowCount,
                        createdAt = requireNotNull(batch.createdAt),
                        startedAt = batch.startedAt,
                        finishedAt = batch.finishedAt,
                        firstDownloadedAt = batch.firstDownloadedAt,
                        downloadExpiresAt = batch.firstDownloadedAt?.plus(retentionProperties.afterDownload.delay),
                        fileDeletedAt = batch.fileDeletedAt,
                        downloadAvailable =
                            batch.status == ExportBatchStatus.COMPLETED && batch.fileKey != null && batch.fileDeletedAt == null,
                    ),
            )
        }.onFailure { logger.warn("Failed to emit export batch event for $id", it) }
    }

    suspend fun publishDeleted(
        userId: UUID,
        batchId: UUID,
    ) {
        runCatching {
            actionEventService.newEvent(
                userId = userId,
                type = ActionEventType.DELETE,
                category = ActionEventCategory.EXPORT_BATCH,
                data = batchId,
            )
        }.onFailure { logger.warn("Failed to emit deleted export batch event for $batchId", it) }
    }
}
