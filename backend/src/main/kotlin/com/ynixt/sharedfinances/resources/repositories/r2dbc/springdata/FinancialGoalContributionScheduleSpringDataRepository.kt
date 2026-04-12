package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalContributionScheduleEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface FinancialGoalContributionScheduleSpringDataRepository : R2dbcRepository<FinancialGoalContributionScheduleEntity, String> {
    fun countByFinancialGoalId(financialGoalId: UUID): Mono<Long>

    fun findAllByFinancialGoalIdOrderByNextExecutionAscIdAsc(
        financialGoalId: UUID,
        pageable: Pageable,
    ): Flux<FinancialGoalContributionScheduleEntity>

    fun findAllByFinancialGoalId(financialGoalId: UUID): Flux<FinancialGoalContributionScheduleEntity>

    fun findByIdAndFinancialGoalId(
        id: UUID,
        financialGoalId: UUID,
    ): Mono<FinancialGoalContributionScheduleEntity>

    @Query(
        """
        SELECT * FROM financial_goal_contribution_schedule
        WHERE next_execution IS NOT NULL AND next_execution <= :date
        """,
    )
    fun findAllByNextExecutionLessThanEqual(date: LocalDate): Flux<FinancialGoalContributionScheduleEntity>
}
