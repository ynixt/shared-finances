package com.ynixt.sharedfinances.application.web.dto.dashboard

import java.math.BigDecimal
import java.util.UUID

data class GroupOverviewDashboardDto(
    val selectedMonth: String,
    val currency: String,
    val cards: List<OverviewDashboardCardDto>,
    val charts: GroupOverviewDashboardChartsDto,
    val debtPairs: List<GroupOverviewDebtPairDto>,
    val goalOverCommittedWarning: Boolean,
)

data class GroupOverviewDashboardChartsDto(
    val cashIn: GroupOverviewDashboardSeriesDto,
    val expense: GroupOverviewDashboardSeriesDto,
    val cashInByCategoryTotal: List<OverviewDashboardPieSliceDto>,
    val cashInByCategoryByMember: List<GroupOverviewDashboardMemberPieDto>,
    val expenseByCategory: List<OverviewDashboardPieSliceDto>,
    val expenseByCategoryByMember: List<GroupOverviewDashboardMemberPieDto>,
    val expenseByMember: List<OverviewDashboardPieSliceDto>,
)

data class GroupOverviewDashboardSeriesDto(
    val total: List<OverviewDashboardChartPointDto>,
    val byMember: List<GroupOverviewDashboardMemberSeriesDto>,
)

data class GroupOverviewDashboardMemberSeriesDto(
    val memberId: UUID,
    val memberName: String,
    val points: List<OverviewDashboardChartPointDto>,
)

data class GroupOverviewDashboardMemberPieDto(
    val memberId: UUID,
    val memberName: String,
    val slices: List<OverviewDashboardPieSliceDto>,
)

data class GroupOverviewDebtPairDto(
    val payerId: UUID,
    val payerName: String,
    val receiverId: UUID,
    val receiverName: String,
    val currency: String,
    val outstandingAmount: BigDecimal,
    val details: List<OverviewDashboardDetailDto>,
)
