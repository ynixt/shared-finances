package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.models.WalletItem
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class NewWalletSourceLeg(
    val walletItemId: UUID,
    val contributionPercent: BigDecimal,
    val billDate: LocalDate? = null,
)

data class ResolvedWalletSourceLeg(
    val walletItemId: UUID,
    val contributionPercent: BigDecimal,
    val walletItem: WalletItem,
    val bill: CreditCardBillEntity? = null,
)
