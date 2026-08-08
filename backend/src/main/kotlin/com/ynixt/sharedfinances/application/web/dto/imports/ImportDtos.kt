package com.ynixt.sharedfinances.application.web.dto.imports

import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletBeneficiaryLegDto
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ImportHashStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ImportHashCheckDto(
    val status: ImportHashStatus,
    val batchId: UUID?,
    val importedAt: OffsetDateTime?,
    val fileName: String?,
)

data class ImportDuplicateLineDto(
    val walletItemId: UUID,
    val name: String?,
    val value: BigDecimal,
    val date: LocalDate,
    val installment: Int?,
)

data class ImportDuplicateCheckDto(
    val lines: List<ImportDuplicateLineDto>,
)

data class ImportLineDto(
    val walletItemId: UUID,
    val name: String?,
    val value: BigDecimal,
    val date: LocalDate,
    val confirmed: Boolean = true,
    val categoryId: UUID?,
    val groupId: UUID?,
    val beneficiaries: List<WalletBeneficiaryLegDto>?,
    val billDate: LocalDate?,
    val installment: Int?,
    val installmentTotal: Int?,
    val createPreviousInstallments: Boolean = false,
    val createFollowingInstallments: Boolean = false,
    val tags: List<String>?,
    val observations: String?,
)

data class CreateImportDto(
    val fileHash: String,
    val fileName: String,
    val format: String = "CSV",
    val lines: List<ImportLineDto>,
)

data class ImportBatchDto(
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
