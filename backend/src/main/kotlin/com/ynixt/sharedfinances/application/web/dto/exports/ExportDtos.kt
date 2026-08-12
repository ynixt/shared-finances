package com.ynixt.sharedfinances.application.web.dto.exports

import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ExportFormat
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class TransactionExportFilterDto(
    val groupId: UUID? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val walletItemIds: Set<UUID> = emptySet(),
    val categoryIds: Set<UUID> = emptySet(),
    val entryTypes: Set<WalletEntryType> = emptySet(),
    val tags: Set<String> = emptySet(),
    val confirmed: Boolean? = null,
    val billDateMode: Boolean = false,
)

data class CreateExportDto(
    val format: ExportFormat,
    val filter: TransactionExportFilterDto,
)

data class ExportBatchDto(
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
