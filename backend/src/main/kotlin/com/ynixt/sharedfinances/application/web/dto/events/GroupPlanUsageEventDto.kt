package com.ynixt.sharedfinances.application.web.dto.events

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import java.util.UUID

data class GroupPlanUsageEventDto(
    val groupId: UUID,
    val quota: PlanLimitKey,
    val usage: Long,
)
