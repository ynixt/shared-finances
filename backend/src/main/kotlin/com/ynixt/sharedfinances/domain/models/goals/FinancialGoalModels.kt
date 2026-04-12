package com.ynixt.sharedfinances.domain.models.goals

import com.ynixt.sharedfinances.domain.enums.GoalLedgerMovementKind
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialGoalHeader(
    val id: UUID,
    val name: String,
    val description: String?,
    val deadline: LocalDate?,
    val ownerUserId: UUID?,
    val groupId: UUID?,
)

data class FinancialGoalTargetAmount(
    val currency: String,
    val targetAmount: BigDecimal,
)

data class GoalLedgerMovementLine(
    val id: UUID,
    val walletItemId: UUID,
    val walletItemName: String,
    val currency: String,
    val signedAmount: BigDecimal,
    val note: String?,
    val movementKind: GoalLedgerMovementKind,
    val scheduleId: UUID?,
    val movementDate: LocalDate,
    val createdAt: OffsetDateTime?,
)

data class GoalContributionScheduleLine(
    val id: UUID,
    val walletItemId: UUID,
    val walletItemName: String,
    val amount: BigDecimal,
    val currency: String,
    val periodicity: RecurrenceType,
    val qtyExecuted: Int,
    val qtyLimit: Int?,
    val lastExecution: LocalDate?,
    val nextExecution: LocalDate?,
    val endExecution: LocalDate?,
    val removesAllocation: Boolean,
)

data class GoalCommitmentMonthlyPoint(
    val yearMonth: String,
    val committedCumulative: BigDecimal,
)

data class GoalCommitmentChartSeries(
    val currency: String,
    val targetAmount: BigDecimal,
    val points: List<GoalCommitmentMonthlyPoint>,
)

data class FinancialGoalDetail(
    val goal: FinancialGoalHeader,
    val targets: List<FinancialGoalTargetAmount>,
    val committedByCurrency: Map<String, BigDecimal>,
    val commitmentChart: List<GoalCommitmentChartSeries>,
)
