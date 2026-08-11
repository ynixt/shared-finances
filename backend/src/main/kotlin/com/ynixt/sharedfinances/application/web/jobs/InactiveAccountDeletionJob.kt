package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.InactiveAccountDeletionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class InactiveAccountDeletionJob(
    private val service: InactiveAccountDeletionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun execute() {
        logger.info("Inactive account cleanup job started")
        val result = service.runCleanup()
        logger.info("Inactive account cleanup job finished: {}", result)
    }
}
