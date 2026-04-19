package com.ynixt.sharedfinances.application.web.dto.walletentry

import java.math.BigDecimal
import java.util.UUID

data class WalletBeneficiaryLegDto(
    val userId: UUID,
    val benefitPercent: BigDecimal,
)
