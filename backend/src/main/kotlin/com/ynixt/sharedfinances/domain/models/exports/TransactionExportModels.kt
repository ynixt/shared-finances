package com.ynixt.sharedfinances.domain.models.exports

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class TransactionExportFilter(
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

data class TransactionExportCursor(
    val date: LocalDate,
    val eventId: UUID,
    val entryId: UUID,
)

data class TransactionExportRow(
    val origin: String,
    val originName: String,
    val date: LocalDate,
    val description: String?,
    val value: BigDecimal,
    val currency: String,
    val category: String?,
    val categoryName: String?,
    val categoryConceptId: String?,
    val group: String?,
    val groupName: String?,
    val installment: String?,
    val beneficiaries: String?,
    val bill: LocalDate?,
    val tags: List<String>,
    val observations: String?,
    val confirmed: Boolean,
    val transactionId: String,
    val transferId: String?,
    val seriesId: String?,
    val cursor: TransactionExportCursor,
)

data class ActiveRecurrenceExportRow(
    val description: String?,
    val paymentType: PaymentType,
    val nextExecution: LocalDate?,
    val category: String?,
    val group: String?,
    val seriesId: UUID,
)
