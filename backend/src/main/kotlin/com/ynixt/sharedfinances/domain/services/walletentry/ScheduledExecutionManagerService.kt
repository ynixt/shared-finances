package com.ynixt.sharedfinances.domain.services.walletentry

import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.ScheduledExecutionManagerRequest
import java.util.UUID

interface ScheduledExecutionManagerService {
    suspend fun list(
        userId: UUID,
        request: ScheduledExecutionManagerRequest,
    ): List<EventListResponse>
}
