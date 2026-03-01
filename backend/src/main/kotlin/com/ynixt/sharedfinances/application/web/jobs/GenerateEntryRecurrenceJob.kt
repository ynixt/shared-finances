package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class GenerateEntryRecurrenceJob(
    private val entryRecurrenceService: EntryRecurrenceService,
) {
    private val logger = LoggerFactory.getLogger(GenerateEntryRecurrenceJob::class.java)

//    @Scheduled(cron = "0 0/1 * * * *")
    @Scheduled(cron = "0 0 0 * * *")
    fun job() {
        logger.info("Generating entry recurrence job started")
        mono {
            entryRecurrenceService
                .queueAllPendingOfExecution()
        }.subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { logger.info("Generating entry recurrence job successfully enqueued $it entries") }
            .doOnError { ex -> logger.error("Generating entry recurrence job failed", ex) }
            .subscribe()
    }
}
