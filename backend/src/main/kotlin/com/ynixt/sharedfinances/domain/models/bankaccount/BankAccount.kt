package com.ynixt.sharedfinances.domain.models.bankaccount

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.WalletItem
import java.math.BigDecimal
import java.util.UUID

class BankAccount(
    name: String,
    enabled: Boolean,
    userId: UUID,
    currency: String,
    balance: BigDecimal,
    showOnDashboard: Boolean = true,
) : WalletItem(
        name = name,
        enabled = enabled,
        userId = userId,
        currency = currency,
        balance = balance,
        showOnDashboard = showOnDashboard,
    ) {
    override val type: WalletItemType = WalletItemType.BANK_ACCOUNT
}
