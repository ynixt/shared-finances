package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.UnconfirmedAccountCleanupService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnconfirmedAccountCleanupJob(
    private val unconfirmedAccountCleanupService: UnconfirmedAccountCleanupService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun execute() {
        logger.info("Unconfirmed account cleanup job started")
        val result = unconfirmedAccountCleanupService.runCleanup()
        logger.info("Unconfirmed account cleanup finished. Removed {} account(s).", result)
    }
}
