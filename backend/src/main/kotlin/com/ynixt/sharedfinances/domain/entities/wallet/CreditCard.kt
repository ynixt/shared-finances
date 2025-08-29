package com.ynixt.sharedfinances.domain.entities.wallet

import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.util.UUID

@Table("credit_card")
class CreditCard(
    name: String,
    enabled: Boolean,
    userId: UUID,
    val totalLimit: BigDecimal,
    val availableLimit: BigDecimal,
    val dueDay: Int,
    val daysBetweenDueAndClosing: Int,
    val dueOnNextBusinessDay: Boolean = true,
) : WalletItem(
        name = name,
        enabled = enabled,
        userId = userId,
    )
