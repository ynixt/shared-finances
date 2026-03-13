package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

abstract class MinimumWalletEventEntity(
    val type: WalletEntryType,
    val name: String?,
    val categoryId: UUID?,
    val userId: UUID?,
    val groupId: UUID?,
    val tags: List<String>?,
    val observations: String?,
    val paymentType: PaymentType,
) : AuditedEntity() {
    @Transient
    var entries: List<MinimumWalletEntryEntity>? = null
}

@Table("wallet_event")
class WalletEventEntity(
    type: WalletEntryType,
    name: String?,
    categoryId: UUID?,
    userId: UUID?,
    groupId: UUID?,
    tags: List<String>?,
    observations: String?,
    val date: LocalDate,
    val confirmed: Boolean,
    val installment: Int?,
    val recurrenceEventId: UUID?,
    paymentType: PaymentType,
) : MinimumWalletEventEntity(
        type = type,
        name = name,
        categoryId = categoryId,
        userId = userId,
        groupId = groupId,
        tags = tags,
        observations = observations,
        paymentType = paymentType,
    )

abstract class MinimumWalletEntryEntity(
    val value: BigDecimal,
    val walletEventId: UUID,
    val walletItemId: UUID,
) : AuditedEntity() {
    @Transient
    var walletItem: WalletItemEntity? = null
}

@Table("wallet_entry")
class WalletEntryEntity(
    value: BigDecimal,
    walletEventId: UUID,
    walletItemId: UUID,
    val billId: UUID?,
) : MinimumWalletEntryEntity(
        value = value,
        walletEventId = walletEventId,
        walletItemId = walletItemId,
    ) {
    @Transient
    var bill: CreditCardBillEntity? = null
}
