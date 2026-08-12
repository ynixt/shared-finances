package com.ynixt.sharedfinances.application.web.dto.exports

import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ExportFormat
import java.time.OffsetDateTime
import java.util.UUID

data class ExportBatchStatusEventDto(
    val id: UUID,
    val format: ExportFormat,
    val status: ExportBatchStatus,
    val errorMessage: String?,
    val rowCount: Int?,
    val createdAt: OffsetDateTime,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val firstDownloadedAt: OffsetDateTime?,
    val downloadExpiresAt: OffsetDateTime?,
    val fileDeletedAt: OffsetDateTime?,
    val downloadAvailable: Boolean,
)
