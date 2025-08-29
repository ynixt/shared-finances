package com.ynixt.sharedfinances.domain.entities.wallet.entries

import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("debit_entry")
class DebitEntry(
    name: String?,
    description: String?,
    value: BigDecimal,
    date: LocalDate,
    categoryId: UUID?,
    userId: UUID?,
    groupId: UUID?,
    tags: List<String>?,
    observations: String?,
    confirmed: Boolean,
    val bankAccountId: UUID,
    val recurrenceConfigId: UUID?,
) : WalletEntry(
        name = name,
        description = description,
        value = value,
        date = date,
        categoryId = categoryId,
        userId = userId,
        groupId = groupId,
        tags = tags,
        observations = observations,
        confirmed = confirmed,
    )
