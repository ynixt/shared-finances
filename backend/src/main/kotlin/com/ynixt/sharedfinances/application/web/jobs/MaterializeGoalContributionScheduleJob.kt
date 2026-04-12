package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.goals.FinancialGoalManagementService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class MaterializeGoalContributionScheduleJob(
    private val financialGoalManagementService: FinancialGoalManagementService,
) {
    private val logger = LoggerFactory.getLogger(MaterializeGoalContributionScheduleJob::class.java)

    @Scheduled(cron = "\${app.jobs.materializeGoal.cron}")
    fun job() {
        logger.info("Materialize goal contribution schedules job started")
        mono {
            financialGoalManagementService.materializeDueSchedules()
        }.subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { logger.info("Materialize goal contribution schedules job finished") }
            .doOnError { ex -> logger.error("Materialize goal contribution schedules job failed", ex) }
            .subscribe()
    }
}
