package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

abstract class MinimumWalletEntry(
    val name: String?,
    val description: String?,
    val value: BigDecimal,
    val categoryId: UUID?,
    val userId: UUID?,
    val groupId: UUID?,
    val tags: List<String>?,
    val observations: String?,
) : AuditedEntity()

abstract class WalletEntry(
    name: String?,
    description: String?,
    value: BigDecimal,
    categoryId: UUID?,
    userId: UUID?,
    groupId: UUID?,
    tags: List<String>?,
    observations: String?,
    val date: LocalDate,
    val confirmed: Boolean,
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
