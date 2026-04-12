package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalTargetEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface FinancialGoalTargetSpringDataRepository : R2dbcRepository<FinancialGoalTargetEntity, String> {
    fun findAllByFinancialGoalId(financialGoalId: UUID): Flux<FinancialGoalTargetEntity>

    @Modifying
    @Query("DELETE FROM financial_goal_target WHERE financial_goal_id = :financialGoalId")
    fun deleteAllByFinancialGoalId(financialGoalId: UUID): Mono<Long>
}
