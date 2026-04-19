package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.exceptions.http.InvalidWalletBeneficiarySplitException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

object WalletBeneficiarySplit {
    private val HUNDRED = BigDecimal("100.00")

    fun validate(raw: List<NewWalletBeneficiaryLeg>) {
        if (raw.isEmpty()) {
            throw InvalidWalletBeneficiarySplitException("At least one beneficiary is required")
        }

        val seen = mutableSetOf<UUID>()
        raw.forEachIndexed { index, beneficiary ->
            val scaled = beneficiary.benefitPercent.setScale(2, RoundingMode.HALF_UP)
            if (scaled < BigDecimal.ZERO.setScale(2) || scaled > HUNDRED) {
                throw InvalidWalletBeneficiarySplitException(
                    "Beneficiary $index percent must be between 0 and 100",
                )
            }
            if (!seen.add(beneficiary.userId)) {
                throw InvalidWalletBeneficiarySplitException(
                    "Beneficiary ${beneficiary.userId} was provided more than once",
                )
            }
        }

        val sum =
            raw.fold(BigDecimal.ZERO.setScale(2)) { acc, beneficiary ->
                acc.add(beneficiary.benefitPercent.setScale(2, RoundingMode.HALF_UP))
            }

        if (sum.compareTo(HUNDRED) != 0) {
            throw InvalidWalletBeneficiarySplitException(
                "Beneficiary percentages must sum to exactly 100 (got ${sum.stripTrailingZeros().toPlainString()})",
            )
        }
    }
}
