package com.ynixt.shared_finances.domain.entities.wallet

import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.util.*

@Table("bank_account")
class BankAccount(
    name: String,
    enabled: Boolean,
    userId: UUID,
    val balance: BigDecimal,
): WalletItem(
    name = name,
    enabled = enabled,
    userId = userId,
) {
}