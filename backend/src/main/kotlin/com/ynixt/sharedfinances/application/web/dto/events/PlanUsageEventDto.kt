package com.ynixt.sharedfinances.application.web.dto.events

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey

data class PlanUsageEventDto(
    val quota: PlanLimitKey,
    val usage: Long,
)
