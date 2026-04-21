package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDashboardDto
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboard

interface GroupOverviewDashboardDtoMapper {
    fun toDto(model: GroupOverviewDashboard): GroupOverviewDashboardDto
}
