package com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemDto
import java.math.BigDecimal
import java.util.UUID

class BankAccountDto(
    id: UUID,
    name: String,
    enabled: Boolean,
    showOnDashboard: Boolean = true,
    userId: UUID,
    currency: String,
    val balance: BigDecimal,
) : WalletItemDto(
        id = id,
        name = name,
        enabled = enabled,
        showOnDashboard = showOnDashboard,
        userId = userId,
        currency = currency,
    )
