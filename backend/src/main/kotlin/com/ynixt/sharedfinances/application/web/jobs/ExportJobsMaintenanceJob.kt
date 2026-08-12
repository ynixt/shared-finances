package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.exports.ExportJobService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class ExportJobsMaintenanceJob(
    private val exportJobService: ExportJobService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        mono { exportJobService.reconcile() }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { logger.error("Export startup reconciliation failed", it) }
            .subscribe()
    }

    suspend fun executeReconcile() {
        exportJobService.reconcile()
    }
}
