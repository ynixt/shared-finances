package com.ynixt.sharedfinances.application.web.dto.dashboard

import java.math.BigDecimal
import java.util.UUID

data class OverviewDashboardDto(
    val selectedMonth: String,
    val currency: String,
    val cards: List<OverviewDashboardCardDto>,
    val charts: OverviewDashboardChartsDto,
    val goalCommittedTotal: BigDecimal,
    val freeBalanceTotal: BigDecimal,
    val goalOverCommittedWarning: Boolean,
)

data class OverviewDashboardCardDto(
    val key: String,
    val value: BigDecimal,
    val details: List<OverviewDashboardDetailDto>,
)

data class OverviewDashboardDetailDto(
    val sourceId: UUID?,
    val sourceType: String,
    val label: String,
    val value: BigDecimal,
    val children: List<OverviewDashboardDetailDto> = emptyList(),
    val accountOverCommitted: Boolean = false,
)

data class OverviewDashboardChartsDto(
    val balance: List<OverviewDashboardChartPointDto>,
    val cashIn: List<OverviewDashboardChartPointDto>,
    val cashOut: List<OverviewDashboardChartPointDto>,
    val expense: List<OverviewDashboardChartPointDto>,
    val cashInByCategory: List<OverviewDashboardPieSliceDto>,
    val cashOutByCategory: List<OverviewDashboardPieSliceDto>,
    val expenseByGroup: List<OverviewDashboardPieSliceDto>,
    val expenseByCategory: List<OverviewDashboardPieSliceDto>,
)

data class OverviewDashboardChartPointDto(
    val month: String,
    val value: BigDecimal,
    val executedValue: BigDecimal,
    val projectedValue: BigDecimal,
)

data class OverviewDashboardPieSliceDto(
    val id: UUID?,
    val label: String,
    val value: BigDecimal,
    val executedValue: BigDecimal,
    val projectedValue: BigDecimal,
)
