package com.ynixt.sharedfinances.domain.services.imports

import java.util.UUID

interface ImportJobService {
    suspend fun processDispatchMessage(batchId: UUID)

    suspend fun dispatchNextQueuedForUser(userId: UUID)

    suspend fun reconcile(): Long
}
