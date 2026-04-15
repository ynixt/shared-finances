package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GenerateEntryRecurrenceJob(
    private val recurrenceService: RecurrenceService,
) {
    private val logger = LoggerFactory.getLogger(GenerateEntryRecurrenceJob::class.java)

    suspend fun execute() {
        logger.info("Generating entry recurrence job started")
        val enqueued = recurrenceService.queueAllPendingOfExecution()
        logger.info("Generating entry recurrence job successfully enqueued $enqueued entries")
    }
}
