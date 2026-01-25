package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.mfa.MfaService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class ExpireMfaJob(
    private val mfaService: MfaService,
) {
    private val logger = LoggerFactory.getLogger(ExpireMfaJob::class.java)

    @Scheduled(cron = "0 0 0/12 * * *")
    fun job() {
        logger.info("Expire mfa job started")

        mfaService
            .expireOld()
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { logger.info("Expire mfa job finished successfully. Expired $it.") }
            .doOnError { ex -> logger.error("Expire mfa job failed", ex) }
            .subscribe()
    }
}
