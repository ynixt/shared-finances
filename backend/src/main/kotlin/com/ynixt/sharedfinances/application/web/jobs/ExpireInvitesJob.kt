package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.groups.GroupInviteService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ExpireInvitesJob(
    private val groupInviteService: GroupInviteService,
) {
    private val logger = LoggerFactory.getLogger(ExpireInvitesJob::class.java)

    suspend fun execute() {
        logger.info("Expire invites job started")
        val expired = groupInviteService.expireOld()
        logger.info("Expire invites job finished successfully. Expired $expired.")
    }
}
