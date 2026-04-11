package com.ynixt.sharedfinances.application.web.dto.wallet.creditCard

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemDto
import java.math.BigDecimal
import java.util.UUID

class CreditCardDto(
    id: UUID,
    name: String,
    enabled: Boolean,
    showOnDashboard: Boolean = true,
    userId: UUID,
    currency: String,
    val totalLimit: BigDecimal,
    val balance: BigDecimal,
    val dueDay: Int,
    val daysBetweenDueAndClosing: Int,
    val dueOnNextBusinessDay: Boolean,
) : WalletItemDto(
        id = id,
        name = name,
        enabled = enabled,
        showOnDashboard = showOnDashboard,
        userId = userId,
        currency = currency,
    )
