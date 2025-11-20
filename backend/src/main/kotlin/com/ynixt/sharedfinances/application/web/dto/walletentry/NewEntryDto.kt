package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class NewEntryDto(
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
)
