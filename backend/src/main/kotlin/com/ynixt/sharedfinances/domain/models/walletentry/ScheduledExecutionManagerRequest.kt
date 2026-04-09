package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter
import java.util.UUID

data class ScheduledExecutionManagerRequest(
    val groupId: UUID?,
    val filter: ScheduledExecutionFilter? = null,
) {
    val filterWithDefault: ScheduledExecutionFilter = filter ?: ScheduledExecutionFilter.FUTURE
}
