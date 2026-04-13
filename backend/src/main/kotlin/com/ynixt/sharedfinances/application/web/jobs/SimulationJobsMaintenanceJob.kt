package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component("simulationJobSchedulingConfig")
class SimulationJobSchedulingConfig(
    @param:Value("\${app.jobs.simulation.reconcile.cron-enabled:false}")
    private val reconcileCronEnabled: Boolean,
    @param:Value("\${app.jobs.simulation.reconcile.cron}")
    private val reconcileCron: String,
) {
    @Suppress("unused")
    val reconcileScheduledCron: String
        get() =
            if (reconcileCronEnabled) {
                reconcileCron
            } else {
                Scheduled.CRON_DISABLED
            }
}

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

    @Scheduled(cron = "#{@simulationJobSchedulingConfig.reconcileScheduledCron}")
    fun reconcileLease() {
        logger.info("Simulation jobs periodic reconciliation started")
        mono {
            simulationJobService.reconcileExpiredLeases()
        }.subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { changed -> logger.info("Simulation jobs periodic reconciliation finished with $changed lease(s) recovered") }
            .doOnError { ex -> logger.error("Simulation jobs periodic reconciliation failed", ex) }
            .subscribe()
    }

    @Scheduled(cron = "\${app.jobs.simulation.purge.cron}")
    fun purge() {
        logger.info("Simulation jobs purge started")
        mono {
            simulationJobService.purgeOldJobs()
        }.subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { deleted -> logger.info("Simulation jobs purge finished with $deleted row(s) removed") }
            .doOnError { ex -> logger.error("Simulation jobs purge failed", ex) }
            .subscribe()
    }
}
