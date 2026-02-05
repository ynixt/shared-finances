package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.models.WalletItem
import java.math.BigDecimal
import java.util.UUID

data class EntrySum(
    val balance: BigDecimal,
    val revenue: BigDecimal,
    val expense: BigDecimal,
) {
    companion object {
        val EMPTY = EntrySum(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
    }
}

data class EntrySumResult(
    val sum: EntrySum,
    val period: EntrySum,
    val projected: EntrySum,
    val walletItemId: UUID,
    val creditCardBillId: UUID?,
) {
    var walletItem: WalletItem? = null
    var user: UserEntity? = null

    companion object {
        fun empty(walletItemId: UUID) =
            EntrySumResult(
                sum = EntrySum.EMPTY,
                period = EntrySum.EMPTY,
                projected = EntrySum.EMPTY,
                walletItemId = walletItemId,
                creditCardBillId = null,
            )
    }
}

operator fun EntrySum.plus(other: EntrySum): EntrySum =
    EntrySum(
        balance = this.balance + other.balance,
        revenue = this.revenue + other.revenue,
        expense = this.expense + other.expense,
    )

operator fun EntrySumResult.plus(other: EntrySumResult): EntrySumResult {
    require(this.walletItemId == other.walletItemId) {
        "Should be for the same wallet item"
    }

    return EntrySumResult(
        sum = this.sum + other.sum,
        projected = this.projected + other.projected,
        period = this.period + other.period,
        walletItemId = this.walletItemId,
        creditCardBillId = this.creditCardBillId,
    )
}
