package com.ynixt.sharedfinances.domain.models.walletentry

import java.math.BigDecimal
import java.util.UUID

data class NewWalletBeneficiaryLeg(
    val userId: UUID,
    val benefitPercent: BigDecimal,
)

data class ResolvedWalletBeneficiaryLeg(
    val userId: UUID,
    val benefitPercent: BigDecimal,
)
