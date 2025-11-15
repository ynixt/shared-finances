package com.ynixt.sharedfinances.domain.models.creditcard

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.WalletItem
import java.math.BigDecimal
import java.util.UUID

class CreditCard(
    name: String,
    enabled: Boolean,
    userId: UUID,
    currency: String,
    val totalLimit: BigDecimal,
    /**
     * Also know as availableLimit
     **/
    balance: BigDecimal,
    val dueDay: Int,
    val daysBetweenDueAndClosing: Int,
    val dueOnNextBusinessDay: Boolean = true,
) : WalletItem(
        name = name,
        enabled = enabled,
        userId = userId,
        currency = currency,
        balance = balance,
    ) {
    override val type: WalletItemType = WalletItemType.CREDIT_CARD
}
