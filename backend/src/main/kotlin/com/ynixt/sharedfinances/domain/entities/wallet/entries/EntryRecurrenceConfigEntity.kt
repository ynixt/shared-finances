package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("entry_recurrence_config")
class EntryRecurrenceConfigEntity(
    name: String?,
    value: BigDecimal,
    categoryId: UUID?,
    userId: UUID?,
    groupId: UUID?,
    tags: List<String>?,
    observations: String?,
    type: WalletEntryType,
    originId: UUID,
    targetId: UUID?,
    val periodicity: RecurrenceType,
    val paymentType: PaymentType,
    val qtyExecuted: Int,
    val qtyLimit: Int?,
    val lastExecution: LocalDate,
    val nextExecution: LocalDate?,
) : MinimumWalletEntry(
        type = type,
        originId = originId,
        targetId = targetId,
        name = name,
        value = value,
        categoryId = categoryId,
        userId = userId,
        groupId = groupId,
        tags = tags,
        observations = observations,
    )
