package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.GroupInviteService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class ExpireInvitesJob(
    private val groupInviteService: GroupInviteService,
) {
    private val logger = LoggerFactory.getLogger(ExpireInvitesJob::class.java)

    @Scheduled(cron = "0 0/30 * * * *")
    fun job() {
        logger.info("Expire invites job started")

        groupInviteService
            .expireOld()
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { logger.info("Expire invites job finished successfully. Expired $it.") }
            .doOnError { ex -> logger.error("Expire invites job failed", ex) }
            .subscribe()
    }
}
