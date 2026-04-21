package com.ynixt.sharedfinances.domain.models.dashboard

import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class GroupOverviewDashboard(
    val selectedMonth: YearMonth,
    val currency: String,
    val cards: List<OverviewDashboardCard>,
    val charts: GroupOverviewDashboardCharts,
    val debtPairs: List<GroupOverviewDebtPair>,
    val goalOverCommittedWarning: Boolean,
)

data class GroupOverviewDashboardCharts(
    val cashIn: GroupOverviewDashboardSeries,
    val expense: GroupOverviewDashboardSeries,
    val cashInByCategoryTotal: List<OverviewDashboardPieSlice>,
    val cashInByCategoryByMember: List<GroupOverviewDashboardMemberPie>,
    val expenseByCategory: List<OverviewDashboardPieSlice>,
    val expenseByCategoryByMember: List<GroupOverviewDashboardMemberPie>,
    val expenseByMember: List<OverviewDashboardPieSlice>,
)

data class GroupOverviewDashboardSeries(
    val total: List<OverviewDashboardChartPoint>,
    val byMember: List<GroupOverviewDashboardMemberSeries>,
)

data class GroupOverviewDashboardMemberSeries(
    val memberId: UUID,
    val memberName: String,
    val points: List<OverviewDashboardChartPoint>,
)

data class GroupOverviewDashboardMemberPie(
    val memberId: UUID,
    val memberName: String,
    val slices: List<OverviewDashboardPieSlice>,
)

data class GroupOverviewDebtPair(
    val payerId: UUID,
    val payerName: String,
    val receiverId: UUID,
    val receiverName: String,
    val currency: String,
    val outstandingAmount: BigDecimal,
    val details: List<OverviewDashboardDetail> = emptyList(),
)
