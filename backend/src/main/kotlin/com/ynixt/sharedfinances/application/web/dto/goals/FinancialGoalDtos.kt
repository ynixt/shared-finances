package com.ynixt.sharedfinances.application.web.dto.goals

import com.ynixt.sharedfinances.domain.enums.GoalLedgerMovementKind
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialGoalTargetDto(
    val currency: String,
    val targetAmount: BigDecimal,
)

data class FinancialGoalSummaryDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val deadline: LocalDate?,
    val ownerUserId: UUID?,
    val groupId: UUID?,
)

data class GoalLedgerMovementDto(
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

data class GoalContributionScheduleDto(
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

data class GoalCommitmentMonthlyPointDto(
    val yearMonth: String,
    val committedCumulative: BigDecimal,
)

data class GoalCommitmentChartSeriesDto(
    val currency: String,
    val targetAmount: BigDecimal,
    val points: List<GoalCommitmentMonthlyPointDto>,
)

data class FinancialGoalDetailDto(
    val goal: FinancialGoalSummaryDto,
    val targets: List<FinancialGoalTargetDto>,
    val committedByCurrency: Map<String, BigDecimal>,
    val commitmentChart: List<GoalCommitmentChartSeriesDto>,
)

data class NewFinancialGoalRequestDto(
    val name: String,
    val description: String?,
    val deadline: LocalDate?,
    val targets: List<FinancialGoalTargetDto>,
)

data class EditFinancialGoalRequestDto(
    val name: String,
    val description: String?,
    val deadline: LocalDate?,
    val targets: List<FinancialGoalTargetDto>,
)

data class GoalAllocateRequestDto(
    val walletItemId: UUID,
    val amount: BigDecimal,
    val allocationDate: LocalDate,
    val note: String? = null,
)

data class GoalReverseRequestDto(
    val walletItemId: UUID,
    val amount: BigDecimal,
    val note: String?,
)

data class EditGoalLedgerMovementRequestDto(
    val newSignedAmount: BigDecimal,
    val allocationDate: LocalDate? = null,
    val note: String? = null,
)

data class NewGoalScheduleRequestDto(
    val walletItemId: UUID,
    val amount: BigDecimal,
    val periodicity: RecurrenceType,
    val firstExecution: LocalDate,
    val qtyLimit: Int?,
    val removesAllocation: Boolean = false,
)

data class EditGoalScheduleRequestDto(
    val walletItemId: UUID,
    val amount: BigDecimal,
    val periodicity: RecurrenceType,
    val nextExecution: LocalDate,
    val qtyLimit: Int?,
    val removesAllocation: Boolean,
)
