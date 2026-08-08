package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class NewEntryRequest(
    val type: WalletEntryType,
    val groupId: UUID? = null,
    /** Required for [WalletEntryType.TRANSFER]; for other types use [sources] (or legacy [originId] only). */
    val originId: UUID? = null,
    val targetId: UUID? = null,
    val name: String? = null,
    val categoryId: UUID? = null,
    val date: LocalDate,
    val value: BigDecimal? = null,
    val originValue: BigDecimal? = null,
    val targetValue: BigDecimal? = null,
    val confirmed: Boolean,
    val observations: String? = null,
    val paymentType: PaymentType,
    val installments: Int? = null,
    val periodicity: RecurrenceType? = null,
    val periodicityQtyLimit: Int? = null,
    /** Zero-based installment offset used when creating a recurrence series segment. */
    val seriesOffset: Int = 0,
    /** Total number of installments in the logical series, independent from this segment length. */
    val seriesQtyTotal: Int? = null,
    /** Existing logical series to which this segment belongs. */
    val seriesId: UUID? = null,
    /** Import batch that owns the event and any recurrence config created from this request. */
    val importBatchId: UUID? = null,
    /** Confirmation state used only while materializing retroactive occurrences during creation. */
    val recurrenceConfirmedOverride: Boolean? = null,
    val originBillDate: LocalDate? = null,
    val targetBillDate: LocalDate? = null,
    val tags: List<String>? = null,
    val transferPurpose: TransferPurpose = TransferPurpose.GENERAL,
    val group: GroupWithRole? = null,
    val origin: WalletItem? = null,
    val originBill: CreditCardBillEntity? = null,
    val target: WalletItem? = null,
    val targetBill: CreditCardBillEntity? = null,
    val category: WalletEntryCategoryEntity? = null,
    val initialBalance: Boolean = false,
    /** Non-transfer: funding legs with percentages summing to 100. */
    val sources: List<NewWalletSourceLeg>? = null,
    /** Populated in [com.ynixt.sharedfinances.resources.services.walletentry.WalletEntrySaveServiceImpl.loadRelationships]. */
    val resolvedSources: List<ResolvedWalletSourceLeg>? = null,
    /** Group non-transfer only: beneficiaries with percentages summing to 100. */
    val beneficiaries: List<NewWalletBeneficiaryLeg>? = null,
    /** Populated in [com.ynixt.sharedfinances.resources.services.walletentry.WalletEntrySaveServiceImpl.loadRelationships]. */
    val resolvedBeneficiaries: List<ResolvedWalletBeneficiaryLeg>? = null,
) {
    val valueFixedForType: BigDecimal? = value?.let { type.fixValue(it) }
    val primaryValue: BigDecimal =
        when (type) {
            WalletEntryType.TRANSFER -> requireNotNull(originValue).abs()
            else -> requireNotNull(value).abs()
        }

    val transferOriginValue: BigDecimal?
        get() = if (type == WalletEntryType.TRANSFER) originValue?.abs() else null

    val transferTargetValue: BigDecimal?
        get() = if (type == WalletEntryType.TRANSFER) targetValue?.abs() else null

    fun isInFuture(today: LocalDate): Boolean = date.isAfter(today)
}
