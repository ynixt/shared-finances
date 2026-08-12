package com.ynixt.sharedfinances.domain.queue.producer

import java.util.UUID

interface ExportJobDispatchQueueProducer {
    fun send(batchId: UUID)
}
