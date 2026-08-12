package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.resources.services.exports.ExportPurgeService
import org.springframework.stereotype.Component

@Component
class ExportPurgeJobs(
    private val purgeService: ExportPurgeService,
) {
    suspend fun purgeAfterDownload() = purgeService.purgeAfterDownload()

    suspend fun purgeByAbsoluteAge() = purgeService.purgeByAbsoluteAge()
}
