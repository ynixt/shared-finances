package com.ynixt.sharedfinances.domain.entities.wallet.entries

import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("entry_installment_config")
class EntryInstallmentConfig(
    name: String?,
    description: String?,
    value: BigDecimal,
    categoryId: UUID?,
    userId: UUID?,
    groupId: UUID?,
    tags: List<String>?,
    observations: String,
    val installments: Int,
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
