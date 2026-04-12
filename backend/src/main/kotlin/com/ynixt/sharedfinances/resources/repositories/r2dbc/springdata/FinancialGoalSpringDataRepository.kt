package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface FinancialGoalSpringDataRepository : R2dbcRepository<FinancialGoalEntity, String> {
    fun findAllByUserIdOrderByNameAscIdAsc(
        userId: UUID,
        pageable: Pageable,
    ): Flux<FinancialGoalEntity>

    fun findAllByGroupIdOrderByNameAscIdAsc(
        groupId: UUID,
        pageable: Pageable,
    ): Flux<FinancialGoalEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<FinancialGoalEntity>

    fun findByIdAndGroupId(
        id: UUID,
        groupId: UUID,
    ): Mono<FinancialGoalEntity>

    @Modifying
    @Query("DELETE FROM financial_goal WHERE id = :id AND user_id = :userId")
    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long>

    fun countByUserId(userId: UUID): Mono<Long>

    fun countByGroupId(groupId: UUID): Mono<Long>
}
