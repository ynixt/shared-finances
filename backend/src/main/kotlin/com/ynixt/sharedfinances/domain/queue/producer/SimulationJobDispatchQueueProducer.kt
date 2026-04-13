package com.ynixt.sharedfinances.domain.queue.producer

import java.util.UUID

interface SimulationJobDispatchQueueProducer {
    fun send(jobId: UUID)
}
