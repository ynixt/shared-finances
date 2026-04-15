package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class SimulationJobsMaintenanceJob(
    private val simulationJobService: SimulationJobService,
) {
    private val logger = LoggerFactory.getLogger(SimulationJobsMaintenanceJob::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        logger.info("Simulation jobs startup reconciliation started")
        mono {
            simulationJobService.reconcileExpiredLeases()
        }.subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { changed -> logger.info("Simulation jobs startup reconciliation finished with $changed lease(s) recovered") }
            .doOnError { ex -> logger.error("Simulation jobs startup reconciliation failed", ex) }
            .subscribe()
    }

    suspend fun executeReconcile() {
        logger.info("Simulation jobs periodic reconciliation started")
        val changed = simulationJobService.reconcileExpiredLeases()
        logger.info("Simulation jobs periodic reconciliation finished with $changed lease(s) recovered")
    }

    suspend fun executePurge() {
        logger.info("Simulation jobs purge started")
        val deleted = simulationJobService.purgeOldJobs()
        logger.info("Simulation jobs purge finished with $deleted row(s) removed")
    }
}
