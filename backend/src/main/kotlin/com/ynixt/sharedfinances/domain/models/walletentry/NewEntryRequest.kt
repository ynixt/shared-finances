package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class NewEntryRequest(
    val type: WalletEntryType,
    val groupId: UUID? = null,
    val originId: UUID,
    val targetId: UUID? = null,
    val name: String? = null,
    val categoryId: UUID? = null,
    val date: LocalDate,
    val value: BigDecimal,
    val confirmed: Boolean,
    val observations: String? = null,
    val paymentType: PaymentType,
    val installments: Int? = null,
    val periodicity: RecurrenceType? = null,
    val periodicityQtyLimit: Int? = null,
    val originBillDate: LocalDate? = null,
    val targetBillDate: LocalDate? = null,
    val tags: List<String>? = null,
    val group: GroupWithRole? = null,
    val origin: WalletItem? = null,
    val originBill: CreditCardBillEntity? = null,
    val target: WalletItem? = null,
    val targetBill: CreditCardBillEntity? = null,
    val category: WalletEntryCategoryEntity? = null,
) {
    val valueFixedForType: BigDecimal = type.fixValue(value)

    fun isInFuture(today: LocalDate): Boolean = date.isAfter(today)
}
