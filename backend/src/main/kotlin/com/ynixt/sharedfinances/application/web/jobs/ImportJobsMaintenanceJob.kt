package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.imports.ImportJobService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class ImportJobsMaintenanceJob(
    private val importJobService: ImportJobService,
) {
    private val logger = LoggerFactory.getLogger(ImportJobsMaintenanceJob::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        mono { importJobService.reconcile() }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { count -> logger.info("Import startup reconciliation recovered {} lease(s)", count) }
            .doOnError { error -> logger.error("Import startup reconciliation failed", error) }
            .subscribe()
    }

    suspend fun executeReconcile() {
        val count = importJobService.reconcile()
        logger.info("Import periodic reconciliation recovered {} lease(s)", count)
    }
}
