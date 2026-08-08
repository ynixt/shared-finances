package com.ynixt.sharedfinances.domain.queue.producer

import java.util.UUID

interface ImportJobDispatchQueueProducer {
    fun send(batchId: UUID)
}
