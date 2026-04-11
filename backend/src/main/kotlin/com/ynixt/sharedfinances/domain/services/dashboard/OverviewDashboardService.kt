package com.ynixt.sharedfinances.domain.services.dashboard

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard
import java.time.YearMonth
import java.util.UUID

interface OverviewDashboardService {
    suspend fun getOverview(
        userId: UUID,
        defaultCurrency: String,
        selectedMonth: YearMonth,
    ): OverviewDashboard
}
