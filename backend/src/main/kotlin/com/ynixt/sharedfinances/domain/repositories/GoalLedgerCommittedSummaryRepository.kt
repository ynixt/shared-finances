package com.ynixt.sharedfinances.domain.repositories

import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.util.UUID

data class GoalCommittedByWalletRow(
    val walletItemId: UUID,
    val currency: String,
    val committed: BigDecimal,
)

data class GoalCommittedByGoalRow(
    val goalId: UUID,
    val goalName: String,
    val walletItemId: UUID,
    val currency: String,
    val committed: BigDecimal,
)

data class GoalCurrencyCommittedRow(
    val currency: String,
    val committed: BigDecimal,
)

interface GoalLedgerCommittedSummaryRepository {
    fun summarizeCommittedByUserGoals(userId: UUID): Flux<GoalCommittedByWalletRow>

    fun summarizeCommittedByUserGoalsDetailed(userId: UUID): Flux<GoalCommittedByGoalRow>

    fun summarizeCommittedByGroupGoals(groupId: UUID): Flux<GoalCommittedByWalletRow>

    fun summarizeCommittedByGroupGoalsDetailed(groupId: UUID): Flux<GoalCommittedByGoalRow>

    fun summarizeCommittedByGoal(goalId: UUID): Flux<GoalCurrencyCommittedRow>
}
