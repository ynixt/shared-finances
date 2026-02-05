package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

abstract class MinimumWalletEntry(
    val userId: UUID?,
    val groupId: UUID?,
    val type: WalletEntryType,
    val originId: UUID,
    val targetId: UUID?,
    val name: String?,
    val categoryId: UUID?,
    val value: BigDecimal,
    val tags: List<String>?,
    val observations: String?,
) : AuditedEntity() {
    @Transient
    var origin: WalletItemEntity? = null

    @Transient
    var target: WalletItemEntity? = null
}

@Table("wallet_entry")
class WalletEntryEntity(
    type: WalletEntryType,
    name: String?,
    value: BigDecimal,
    categoryId: UUID?,
    userId: UUID?,
    groupId: UUID?,
    tags: List<String>?,
    observations: String?,
    originId: UUID,
    targetId: UUID?,
    val date: LocalDate,
    val confirmed: Boolean,
    val installment: Int?,
    val recurrenceConfigId: UUID?,
    val originBillId: UUID?,
    val targetBillId: UUID?,
) : MinimumWalletEntry(
        originId = originId,
        targetId = targetId,
        type = type,
        name = name,
        value = value,
        categoryId = categoryId,
        userId = userId,
        groupId = groupId,
        tags = tags,
        observations = observations,
    ) {
    @Transient
    var originBill: CreditCardBillEntity? = null

    @Transient
    var targetBill: CreditCardBillEntity? = null
}
