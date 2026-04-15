package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.UnconfirmedAccountCleanupService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class UnconfirmedAccountCleanupJob(
    private val unconfirmedAccountCleanupService: UnconfirmedAccountCleanupService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.jobs.unconfirmedAccountCleanup.cron}")
    fun job() {
        mono { unconfirmedAccountCleanupService.runCleanup() }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { deletedOrSkipped ->
                if (deletedOrSkipped == null) {
                    return@doOnSuccess
                }
                logger.info("Unconfirmed account cleanup finished. Removed {} account(s).", deletedOrSkipped)
            }.doOnError { ex -> logger.error("Unconfirmed account cleanup job failed", ex) }
            .subscribe()
    }
}
