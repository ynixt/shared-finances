package com.ynixt.sharedfinances.domain.entities.imports

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Table("import_batch")
class ImportBatchEntity(
    val userId: UUID,
    val fileHash: String,
    val fileName: String,
    val format: String,
    val walletItemId: UUID?,
    val qty: Int,
    val totalCredit: BigDecimal,
    val totalDebit: BigDecimal,
    val status: ImportBatchStatus,
    val requestPayload: String?,
    val errorMessage: String?,
    val leaseExpiresAt: OffsetDateTime?,
    val workerId: String?,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val retries: Int,
) : AuditedEntity()
