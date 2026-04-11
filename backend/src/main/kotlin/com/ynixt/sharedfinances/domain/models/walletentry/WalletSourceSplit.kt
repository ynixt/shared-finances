package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidWalletSourceSplitException
import java.math.BigDecimal
import java.math.RoundingMode

object WalletSourceSplit {
    private val HUNDRED = BigDecimal("100.00")

    fun validatePercentsSumExactly100(percents: List<BigDecimal>) {
        if (percents.isEmpty()) {
            throw InvalidWalletSourceSplitException("At least one source is required")
        }
        percents.forEachIndexed { index, p ->
            val scaled = p.setScale(2, RoundingMode.HALF_UP)
            if (scaled < BigDecimal.ZERO.setScale(2) || scaled > HUNDRED) {
                throw InvalidWalletSourceSplitException("Source $index percent must be between 0 and 100")
            }
        }
        val sum = percents.fold(BigDecimal.ZERO.setScale(2)) { acc, p -> acc.add(p.setScale(2, RoundingMode.HALF_UP)) }
        if (sum.compareTo(HUNDRED) != 0) {
            throw InvalidWalletSourceSplitException(
                "Source percentages must sum to exactly 100 (got ${sum.stripTrailingZeros().toPlainString()})",
            )
        }
    }

    /**
     * Distributes [totalMagnitude] (always positive, UI total) across legs; returns signed values per [type].
     * Last leg absorbs rounding remainder.
     */
    fun distributeLegValues(
        type: WalletEntryType,
        totalMagnitude: BigDecimal,
        percents: List<BigDecimal>,
    ): List<BigDecimal> {
        require(totalMagnitude >= BigDecimal.ZERO)
        require(percents.isNotEmpty())

        val absTotal = totalMagnitude.setScale(2, RoundingMode.HALF_UP)
        val n = percents.size
        val portions = mutableListOf<BigDecimal>()
        var allocated = BigDecimal.ZERO.setScale(2)

        if (n == 1) {
            val v = type.fixValue(absTotal)
            return listOf(v)
        }

        for (i in 0 until n - 1) {
            val raw = absTotal.multiply(percents[i].setScale(2, RoundingMode.HALF_UP)).divide(HUNDRED, 2, RoundingMode.HALF_UP)
            allocated = allocated.add(raw)
            portions.add(raw)
        }

        val lastAbs = absTotal.subtract(allocated).setScale(2, RoundingMode.HALF_UP)

        portions.add(lastAbs)

        return portions.map { magnitude ->
            when (type) {
                WalletEntryType.EXPENSE -> magnitude.negate()
                WalletEntryType.REVENUE -> magnitude
                WalletEntryType.TRANSFER -> error("TRANSFER does not use source split")
            }
        }
    }
}
