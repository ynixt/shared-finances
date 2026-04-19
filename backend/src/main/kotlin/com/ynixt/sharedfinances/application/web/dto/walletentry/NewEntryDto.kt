package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class NewEntryDto(
    val type: WalletEntryType,
    val groupId: UUID?,
    /** Required for [WalletEntryType.TRANSFER]. */
    val originId: UUID? = null,
    val targetId: UUID?,
    /** Required for non-transfer types (sum of percents = 100). */
    val sources: List<WalletSourceLegDto>? = null,
    /** Group non-transfer only (sum of percents = 100). */
    val beneficiaries: List<WalletBeneficiaryLegDto>? = null,
    val name: String?,
    val categoryId: UUID?,
    val date: LocalDate,
    val value: BigDecimal?,
    val originValue: BigDecimal?,
    val targetValue: BigDecimal?,
    val confirmed: Boolean,
    val observations: String?,
    val paymentType: PaymentType,
    val installments: Int?,
    val periodicity: RecurrenceType?,
    val periodicityQtyLimit: Int?,
    val originBillDate: LocalDate?,
    val targetBillDate: LocalDate?,
    val tags: List<String>?,
    val transferPurpose: TransferPurpose? = null,
)
