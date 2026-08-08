package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.application.web.dto.imports.ImportBatchStatusEventDto
import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ImportBatchEventPublisherTest {
    @Test
    fun `should emit the authorized user import status payload`() =
        runBlocking {
            val actionEvents = CapturingActionEventService()
            val publisher = ImportBatchEventPublisher(actionEvents)
            val batchId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val startedAt = OffsetDateTime.parse("2026-08-07T12:00:00Z")
            val batch =
                ImportBatchEntity(
                    userId = userId,
                    fileHash = "a".repeat(64),
                    fileName = "statement.csv",
                    format = "CSV",
                    walletItemId = null,
                    qty = 2,
                    totalCredit = BigDecimal.ZERO,
                    totalDebit = BigDecimal.TEN,
                    status = ImportBatchStatus.RUNNING,
                    requestPayload = "{}",
                    errorMessage = null,
                    leaseExpiresAt = startedAt.plusMinutes(1),
                    workerId = "worker",
                    startedAt = startedAt,
                    finishedAt = null,
                    retries = 1,
                ).apply { id = batchId }

            publisher.publish(batch, ActionEventType.UPDATE)

            assertThat(actionEvents.userId).isEqualTo(userId)
            assertThat(actionEvents.type).isEqualTo(ActionEventType.UPDATE)
            assertThat(actionEvents.category).isEqualTo(ActionEventCategory.IMPORT_BATCH)
            assertThat(actionEvents.data).isEqualTo(
                ImportBatchStatusEventDto(
                    id = batchId,
                    status = ImportBatchStatus.RUNNING,
                    errorMessage = null,
                    startedAt = startedAt,
                    finishedAt = null,
                    retries = 1,
                ),
            )
            Unit
        }

    @Test
    fun `should emit the batch id when asynchronous undo completes`() =
        runBlocking {
            val actionEvents = CapturingActionEventService()
            val publisher = ImportBatchEventPublisher(actionEvents)
            val batchId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val batch =
                ImportBatchEntity(
                    userId = userId,
                    fileHash = "b".repeat(64),
                    fileName = "statement.csv",
                    format = "CSV",
                    walletItemId = null,
                    qty = 1,
                    totalCredit = BigDecimal.ZERO,
                    totalDebit = BigDecimal.ONE,
                    status = ImportBatchStatus.UNDO_RUNNING,
                    requestPayload = null,
                    errorMessage = null,
                    leaseExpiresAt = null,
                    workerId = "worker",
                    startedAt = null,
                    finishedAt = null,
                    retries = 1,
                ).apply { id = batchId }

            publisher.publishDeleted(batch)

            assertThat(actionEvents.userId).isEqualTo(userId)
            assertThat(actionEvents.type).isEqualTo(ActionEventType.DELETE)
            assertThat(actionEvents.category).isEqualTo(ActionEventCategory.IMPORT_BATCH)
            assertThat(actionEvents.data).isEqualTo(batchId.toString())
        }

    private class CapturingActionEventService : ActionEventService {
        lateinit var userId: UUID
        lateinit var type: ActionEventType
        lateinit var category: ActionEventCategory
        var data: Any? = null

        override fun getDestinationForUser(userId: UUID): String = "user:$userId"

        override fun getDestinationForGroup(userId: UUID): String = "group:$userId"

        override suspend fun <T> newEvent(
            userId: UUID,
            type: ActionEventType,
            category: ActionEventCategory,
            data: T,
            groupInfo: NewEventGroupInfo?,
        ) {
            this.userId = userId
            this.type = type
            this.category = category
            this.data = data
        }
    }
}
