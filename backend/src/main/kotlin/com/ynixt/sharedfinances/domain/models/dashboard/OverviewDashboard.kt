package com.ynixt.sharedfinances.domain.models.dashboard

import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class OverviewDashboard(
    val selectedMonth: YearMonth,
    val currency: String,
    val cards: List<OverviewDashboardCard>,
    val charts: OverviewDashboardCharts,
    val goalCommittedTotal: BigDecimal,
    val freeBalanceTotal: BigDecimal,
    val goalOverCommittedWarning: Boolean,
)

data class OverviewDashboardCard(
    val key: OverviewDashboardCardKey,
    val value: BigDecimal,
    val details: List<OverviewDashboardDetail>,
)

data class OverviewDashboardDetail(
    val sourceId: UUID?,
    val sourceType: OverviewDashboardDetailSourceType,
    val label: String,
    val value: BigDecimal,
    val children: List<OverviewDashboardDetail> = emptyList(),
    val accountOverCommitted: Boolean = false,
)

data class OverviewDashboardCharts(
    val balance: List<OverviewDashboardChartPoint>,
    val cashIn: List<OverviewDashboardChartPoint>,
    val cashOut: List<OverviewDashboardChartPoint>,
    val expense: List<OverviewDashboardChartPoint>,
    val cashInByCategory: List<OverviewDashboardPieSlice>,
    val cashOutByCategory: List<OverviewDashboardPieSlice>,
    val expenseByGroup: List<OverviewDashboardPieSlice>,
    val expenseByCategory: List<OverviewDashboardPieSlice>,
)

data class OverviewDashboardChartPoint(
    val month: YearMonth,
    val value: BigDecimal,
)

data class OverviewDashboardPieSlice(
    val id: UUID?,
    val label: String,
    val value: BigDecimal,
)

enum class OverviewDashboardCardKey {
    BALANCE,
    GOAL_COMMITTED,
    GOAL_FREE_BALANCE,
    PERIOD_CASH_IN,
    PERIOD_CASH_OUT,
    PERIOD_NET_CASH_FLOW,
    PROJECTED_CASH_IN,
    PROJECTED_CASH_OUT,
    END_OF_PERIOD_BALANCE,
    END_OF_PERIOD_NET_CASH_FLOW,
}

enum class OverviewDashboardDetailSourceType {
    BANK_ACCOUNT,
    CREDIT_CARD_BILL,
    GOAL,
    FORMULA,
}
