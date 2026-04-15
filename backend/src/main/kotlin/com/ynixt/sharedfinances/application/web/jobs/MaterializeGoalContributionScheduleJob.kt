package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.goals.FinancialGoalManagementService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MaterializeGoalContributionScheduleJob(
    private val financialGoalManagementService: FinancialGoalManagementService,
) {
    private val logger = LoggerFactory.getLogger(MaterializeGoalContributionScheduleJob::class.java)

    suspend fun execute() {
        logger.info("Materialize goal contribution schedules job started")
        financialGoalManagementService.materializeDueSchedules()
        logger.info("Materialize goal contribution schedules job finished")
    }
}
