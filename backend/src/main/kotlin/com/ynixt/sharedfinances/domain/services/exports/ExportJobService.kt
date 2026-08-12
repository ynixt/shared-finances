package com.ynixt.sharedfinances.domain.services.exports

import java.util.UUID

interface ExportJobService {
    suspend fun processDispatchMessage(batchId: UUID)

    suspend fun dispatchNextQueuedForUser(userId: UUID)

    suspend fun reconcile(): Long
}
