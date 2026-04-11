package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("recurrence_event")
class RecurrenceEventEntity(
    name: String?,
    categoryId: UUID?,
    userId: UUID?,
    groupId: UUID?,
    tags: List<String>?,
    observations: String?,
    type: WalletEntryType,
    val periodicity: RecurrenceType,
    paymentType: PaymentType,
    val qtyExecuted: Int,
    val qtyLimit: Int?,
    val lastExecution: LocalDate?,
    val nextExecution: LocalDate?,
    val endExecution: LocalDate?,
    val seriesId: UUID,
    val seriesOffset: Int,
    initialBalance: Boolean = false,
) : MinimumWalletEventEntity(
        type = type,
        name = name,
        categoryId = categoryId,
        userId = userId,
        groupId = groupId,
        tags = tags,
        observations = observations,
        paymentType = paymentType,
        initialBalance = initialBalance,
    ) {
    @Transient
    var seriesQtyTotal: Int? = null
}

@Table("recurrence_entry")
class RecurrenceEntryEntity(
    value: BigDecimal,
    walletEventId: UUID,
    walletItemId: UUID,
    val nextBillDate: LocalDate?,
    val lastBillDate: LocalDate?,
) : MinimumWalletEntryEntity(
        value = value,
        walletEventId = walletEventId,
        walletItemId = walletItemId,
    )
