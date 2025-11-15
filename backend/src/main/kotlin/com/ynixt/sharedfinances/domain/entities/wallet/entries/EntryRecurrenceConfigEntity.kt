package com.ynixt.sharedfinances.domain.entities.wallet.entries

import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

@Table("entry_recurrence_config")
class EntryRecurrenceConfigEntity(
    name: String?,
    description: String?,
    value: BigDecimal,
    categoryId: UUID?,
    userId: UUID?,
    groupId: UUID?,
    tags: List<String>?,
    observations: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val type: RecurrenceType,
    val recurrenceQty: Int,
    val lastExecution: LocalDate,
    val nextExecution: LocalDate,
) : MinimumWalletEntry(
        name = name,
        description = description,
        value = value,
        categoryId = categoryId,
        userId = userId,
        groupId = groupId,
        tags = tags,
        observations = observations,
    )
