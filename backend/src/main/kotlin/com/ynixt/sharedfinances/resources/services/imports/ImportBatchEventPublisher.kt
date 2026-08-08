package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.application.web.dto.imports.ImportBatchStatusEventDto
import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ImportBatchEventPublisher(
    private val actionEventService: ActionEventService,
) {
    private val logger = LoggerFactory.getLogger(ImportBatchEventPublisher::class.java)

    suspend fun publish(
        batch: ImportBatchEntity,
        type: ActionEventType,
    ) {
        val batchId = requireNotNull(batch.id) { "import batch id" }
        val event =
            ImportBatchStatusEventDto(
                id = batchId,
                status = batch.status,
                errorMessage = batch.errorMessage,
                startedAt = batch.startedAt,
                finishedAt = batch.finishedAt,
                retries = batch.retries,
            )

        runCatching {
            actionEventService.newEvent(
                userId = batch.userId,
                type = type,
                category = ActionEventCategory.IMPORT_BATCH,
                data = event,
            )
        }.onFailure { error ->
            logger.warn("Failed to emit import batch event for $batchId", error)
        }
    }

    suspend fun publishDeleted(batch: ImportBatchEntity) {
        val batchId = requireNotNull(batch.id) { "import batch id" }
        runCatching {
            actionEventService.newEvent(
                userId = batch.userId,
                type = ActionEventType.DELETE,
                category = ActionEventCategory.IMPORT_BATCH,
                data = batchId.toString(),
            )
        }.onFailure { error ->
            logger.warn("Failed to emit import batch delete event for $batchId", error)
        }
    }
}
