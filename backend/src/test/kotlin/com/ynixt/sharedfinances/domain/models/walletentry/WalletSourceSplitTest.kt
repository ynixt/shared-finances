package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidWalletSourceSplitException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class WalletSourceSplitTest {
    @Test
    fun `distributes expense total with last leg absorbing rounding`() {
        val legs =
            WalletSourceSplit.distributeLegValues(
                type = WalletEntryType.EXPENSE,
                totalMagnitude = BigDecimal("100.00"),
                percents = listOf(BigDecimal("33.33"), BigDecimal("33.33"), BigDecimal("33.34")),
            )
        assertThat(legs).hasSize(3)
        val sum = legs.fold(BigDecimal.ZERO) { a, v -> a.add(v) }
        assertThat(sum).isEqualByComparingTo(BigDecimal("-100.00"))
    }

    @Test
    fun `rejects percent sum not 100`() {
        assertThrows<InvalidWalletSourceSplitException> {
            WalletSourceSplit.validatePercentsSumExactly100(
                listOf(BigDecimal("50.00"), BigDecimal("40.00")),
            )
        }
    }
}
