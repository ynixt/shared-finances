package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter
import java.util.UUID

data class ScheduledExecutionManagerRequestDto(
    val groupId: UUID?,
    val filter: ScheduledExecutionFilter? = null,
)
