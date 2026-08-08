package com.ynixt.sharedfinances.domain.models.imports

import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ImportHashStatus
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletBeneficiaryLeg
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ImportHashCheck(
    val status: ImportHashStatus,
    val batchId: UUID? = null,
    val importedAt: OffsetDateTime? = null,
    val fileName: String? = null,
)

data class ImportDuplicateLine(
    val walletItemId: UUID,
    val name: String?,
    val value: BigDecimal,
    val date: LocalDate,
    val installment: Int? = null,
    val externalTransactionId: String? = null,
)

data class ImportDuplicateCheck(
    val lines: List<ImportDuplicateLine>,
)

data class ImportLine(
    val walletItemId: UUID,
    val name: String?,
    val value: BigDecimal,
    val date: LocalDate,
    val confirmed: Boolean = true,
    val categoryId: UUID? = null,
    val groupId: UUID? = null,
    val beneficiaries: List<NewWalletBeneficiaryLeg>? = null,
    val billDate: LocalDate? = null,
    val installment: Int? = null,
    val installmentTotal: Int? = null,
    val createPreviousInstallments: Boolean = false,
    val createFollowingInstallments: Boolean = false,
    val tags: List<String>? = null,
    val observations: String? = null,
    val externalTransactionId: String? = null,
)

data class CreateImport(
    val fileHash: String,
    val fileName: String,
    val format: String = "CSV",
    val lines: List<ImportLine>,
)

data class ImportBatchSummary(
    val id: UUID,
    val fileHash: String,
    val fileName: String,
    val format: String,
    val walletItemId: UUID?,
    val walletItemName: String,
    val qty: Int,
    val totalCredit: BigDecimal,
    val totalDebit: BigDecimal,
    val status: ImportBatchStatus,
    val errorMessage: String?,
    val createdAt: OffsetDateTime,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val retries: Int,
)

data class ImportJobDispatchMessage(
    val batchId: UUID,
)

sealed interface UndoImportResult {
    data class Accepted(
        val batch: ImportBatchSummary,
    ) : UndoImportResult

    data object NotFound : UndoImportResult

    data object InvalidStatus : UndoImportResult
}
