package com.ynixt.sharedfinances.application.web.dto.walletentry

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class WalletSourceLegDto(
    val walletItemId: UUID,
    val contributionPercent: BigDecimal,
    val billDate: LocalDate? = null,
)
