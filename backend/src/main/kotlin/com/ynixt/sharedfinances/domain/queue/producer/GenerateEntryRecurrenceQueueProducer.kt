package com.ynixt.sharedfinances.domain.queue.producer

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto

interface GenerateEntryRecurrenceQueueProducer {
    fun send(request: GenerateEntryRecurrenceRequestDto)
}
