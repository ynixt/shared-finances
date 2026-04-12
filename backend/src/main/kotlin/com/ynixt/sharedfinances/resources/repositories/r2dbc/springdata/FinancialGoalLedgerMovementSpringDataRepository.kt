package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalLedgerMovementEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface FinancialGoalLedgerMovementSpringDataRepository : R2dbcRepository<FinancialGoalLedgerMovementEntity, String> {
    fun countByFinancialGoalId(financialGoalId: UUID): Mono<Long>

    fun findAllByFinancialGoalIdOrderByMovementDateDescIdDesc(
        financialGoalId: UUID,
        pageable: Pageable,
    ): Flux<FinancialGoalLedgerMovementEntity>

    fun findByIdAndFinancialGoalId(
        id: UUID,
        financialGoalId: UUID,
    ): Mono<FinancialGoalLedgerMovementEntity>

    fun findAllByFinancialGoalIdOrderByMovementDateAscIdAsc(financialGoalId: UUID): Flux<FinancialGoalLedgerMovementEntity>
}
