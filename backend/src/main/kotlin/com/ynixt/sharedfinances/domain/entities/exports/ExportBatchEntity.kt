package com.ynixt.sharedfinances.domain.entities.exports

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ExportFormat
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("export_batch")
class ExportBatchEntity(
    val userId: UUID,
    val status: ExportBatchStatus,
    val format: ExportFormat,
    val filterPayload: String,
    val rowCount: Int?,
    val countedAt: OffsetDateTime?,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val firstDownloadedAt: OffsetDateTime?,
    val fileKey: String?,
    val fileDeletedAt: OffsetDateTime?,
    val errorMessage: String?,
    val leaseExpiresAt: OffsetDateTime?,
    val workerId: String?,
    val retries: Int,
) : AuditedEntity()
