package com.ynixt.sharedfinances.domain.entities.wallet

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.util.UUID

@Table("wallet_item")
class WalletItemEntity(
    val type: WalletItemType,
    val name: String,
    val enabled: Boolean,
    val userId: UUID,
    val currency: String,
    val balance: BigDecimal,
    val totalLimit: BigDecimal?,
    val dueDay: Int?,
    val daysBetweenDueAndClosing: Int?,
    val dueOnNextBusinessDay: Boolean?,
) : AuditedEntity() {
    @Transient
    var user: UserEntity? = null
}
