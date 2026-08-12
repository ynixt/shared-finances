package com.ynixt.sharedfinances.domain.services.exports

import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.exports.CreateExport
import com.ynixt.sharedfinances.domain.models.exports.ExportBatchSummary
import org.springframework.core.io.Resource
import java.time.OffsetDateTime
import java.util.UUID

data class ExportDownload(
    val resource: Resource,
    val fileName: String,
    val contentType: String,
    val firstDownloadedAt: OffsetDateTime,
    val downloadExpiresAt: OffsetDateTime,
)

interface ExportService {
    suspend fun create(
        userId: UUID,
        role: UserPlanRole,
        request: CreateExport,
    ): ExportBatchSummary

    suspend fun get(
        userId: UUID,
        batchId: UUID,
    ): ExportBatchSummary?

    suspend fun list(userId: UUID): List<ExportBatchSummary>

    suspend fun download(
        userId: UUID,
        batchId: UUID,
    ): ExportDownload?

    suspend fun delete(
        userId: UUID,
        batchId: UUID,
    ): Boolean
}
