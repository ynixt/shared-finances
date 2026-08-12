package com.ynixt.sharedfinances.domain.models.exports

import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ExportFormat
import java.time.OffsetDateTime
import java.util.UUID

data class CreateExport(
    val format: ExportFormat,
    val filter: TransactionExportFilter,
)

data class ExportBatchSummary(
    val id: UUID,
    val format: ExportFormat,
    val status: ExportBatchStatus,
    val rowCount: Int?,
    val errorMessage: String?,
    val createdAt: OffsetDateTime,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val firstDownloadedAt: OffsetDateTime?,
    val downloadExpiresAt: OffsetDateTime?,
    val fileDeletedAt: OffsetDateTime?,
    val downloadAvailable: Boolean,
)

data class ExportJobDispatchMessage(
    val batchId: UUID,
)
