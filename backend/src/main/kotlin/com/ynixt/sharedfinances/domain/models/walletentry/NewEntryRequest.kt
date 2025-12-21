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
    val groupId: UUID?,
    val originId: UUID,
    val targetId: UUID?,
    val name: String?,
    val categoryId: UUID?,
    val date: LocalDate,
    val value: BigDecimal,
    val confirmed: Boolean,
    val observations: String?,
    val paymentType: PaymentType,
    val installments: Int?,
    val periodicity: RecurrenceType?,
    val periodicityQtyLimit: Int?,
    val originBillDate: LocalDate?,
    val targetBillDate: LocalDate?,
    val tags: List<String>?,
    val group: GroupWithRole? = null,
    val origin: WalletItem? = null,
    val originBill: CreditCardBillEntity? = null,
    val target: WalletItem? = null,
    val targetBill: CreditCardBillEntity? = null,
    val category: WalletEntryCategoryEntity? = null,
) {
    val valueFixedForType: BigDecimal =
        if (type == WalletEntryType.TRANSFER || type == WalletEntryType.EXPENSE) {
            value.unaryMinus()
        } else {
            value.abs()
        }

    val inFuture = date.isAfter(LocalDate.now())
}
