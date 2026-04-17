package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardPieSlice
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseSourceSummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal const val MAX_NAMED_BREAKDOWN_SLICES = 9
internal const val PREDEFINED_INDIVIDUAL_LABEL = "PREDEFINED_INDIVIDUAL"
internal const val PREDEFINED_UNCATEGORIZED_LABEL = "PREDEFINED_UNCATEGORIZED"
internal const val PREDEFINED_OTHERS_LABEL = "PREDEFINED_OTHERS"

internal data class OverviewDashboardVisibleItems(
    val items: List<WalletItem>,
    val bankAccounts: List<BankAccount>,
    val creditCards: List<CreditCard>,
    val walletItemIds: Set<UUID>,
    val bankAccountById: Map<UUID, BankAccount>,
    val bankAccountIds: Set<UUID>,
)

internal data class MonthlyAmount(
    val net: BigDecimal,
    val cashIn: BigDecimal,
    val cashOut: BigDecimal,
) {
    companion object {
        val ZERO = MonthlyAmount(net = BigDecimal.ZERO, cashIn = BigDecimal.ZERO, cashOut = BigDecimal.ZERO)
    }

    operator fun plus(other: MonthlyAmount): MonthlyAmount =
        MonthlyAmount(
            net = net + other.net,
            cashIn = cashIn + other.cashIn,
            cashOut = cashOut + other.cashOut,
        )
}

internal data class ProjectedCreditCardExpense(
    val creditCardId: UUID,
    val creditCardName: String,
    val currency: String,
    val projectedExpense: BigDecimal,
)

internal data class CreditCardProjectedBalanceAdjustment(
    val currency: String,
    val amount: BigDecimal,
)

internal data class RawValue(
    val key: String,
    val value: BigDecimal,
    val currency: String,
    val referenceDate: LocalDate,
)

internal data class RawDetail(
    val sourceId: UUID?,
    val sourceType: OverviewDashboardDetailSourceType,
    val label: String,
    val value: BigDecimal,
    val currency: String,
    val referenceDate: LocalDate,
    val walletItemId: UUID? = null,
    val key: String = UUID.randomUUID().toString(),
)

internal data class RawChartContribution(
    val chartSeries: ChartSeries,
    val component: ChartPointComponent,
    val month: YearMonth,
    val value: BigDecimal,
    val currency: String,
    val referenceDate: LocalDate,
    val key: String = UUID.randomUUID().toString(),
)

internal data class RawBreakdownContribution(
    val breakdownType: BreakdownType,
    val component: ChartPointComponent,
    val sliceId: UUID?,
    val label: String,
    val value: BigDecimal,
    val currency: String,
    val referenceDate: LocalDate,
    val key: String = UUID.randomUUID().toString(),
)

internal data class BreakdownSliceKey(
    val breakdownType: BreakdownType,
    val sliceId: UUID?,
    val label: String,
)

internal enum class ChartSeries {
    BALANCE,
    CASH_IN,
    CASH_OUT,
    EXPENSE,
}

internal enum class ChartPointComponent {
    EXECUTED,
    PROJECTED,
}

internal data class ChartValueComponents(
    val executed: BigDecimal,
    val projected: BigDecimal,
) {
    fun total(): BigDecimal = executed.add(projected)

    companion object {
        val ZERO = ChartValueComponents(executed = BigDecimal.ZERO, projected = BigDecimal.ZERO)
    }
}

internal data class BreakdownValueComponents(
    val executed: BigDecimal,
    val projected: BigDecimal,
) {
    fun total(): BigDecimal = executed.add(projected)

    companion object {
        val ZERO = BreakdownValueComponents(executed = BigDecimal.ZERO, projected = BigDecimal.ZERO)
    }
}

internal data class ProjectedExpenseContributions(
    val chartContributions: List<RawChartContribution>,
    val breakdownContributions: List<RawBreakdownContribution>,
    val selectedMonthDetails: List<OverviewExpenseSourceSummary>,
) {
    companion object {
        val EMPTY =
            ProjectedExpenseContributions(
                chartContributions = emptyList(),
                breakdownContributions = emptyList(),
                selectedMonthDetails = emptyList(),
            )
    }
}

internal enum class BreakdownType {
    CASH_IN_CATEGORY,
    CASH_OUT_CATEGORY,
    EXPENSE_GROUP,
    EXPENSE_CATEGORY,
}

internal fun Map<YearMonth, Map<UUID, MonthlyAmount>>.getMonthAmount(
    month: YearMonth,
    walletItemId: UUID,
): MonthlyAmount = this[month]?.get(walletItemId) ?: MonthlyAmount.ZERO

internal fun BigDecimal.asMoney(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

internal fun OverviewDashboardPieSlice.sliceIdentity(): Pair<UUID?, String> = id to label

internal fun buildMonthRange(
    start: YearMonth,
    end: YearMonth,
): List<YearMonth> {
    val result = mutableListOf<YearMonth>()
    var cursor = start

    while (!cursor.isAfter(end)) {
        result.add(cursor)
        cursor = cursor.plusMonths(1)
    }

    return result
}

internal fun detailSourceTypeForWalletItemType(walletItemType: WalletItemType): OverviewDashboardDetailSourceType =
    when (walletItemType) {
        WalletItemType.BANK_ACCOUNT -> OverviewDashboardDetailSourceType.BANK_ACCOUNT
        WalletItemType.CREDIT_CARD -> OverviewDashboardDetailSourceType.CREDIT_CARD_BILL
    }
