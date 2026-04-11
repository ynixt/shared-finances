package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardDto
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard

interface OverviewDashboardDtoMapper {
    fun toDto(model: OverviewDashboard): OverviewDashboardDto
}
