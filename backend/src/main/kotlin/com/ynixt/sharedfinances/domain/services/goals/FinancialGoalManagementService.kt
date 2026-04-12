package com.ynixt.sharedfinances.domain.services.goals

import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalDetail
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalHeader
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalTargetAmount
import com.ynixt.sharedfinances.domain.models.goals.GoalContributionScheduleLine
import com.ynixt.sharedfinances.domain.models.goals.GoalLedgerMovementLine
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class NewFinancialGoalInput(
    val name: String,
    val description: String?,
    val deadline: LocalDate?,
    val targets: List<FinancialGoalTargetAmount>,
)

data class EditFinancialGoalInput(
    val name: String,
    val description: String?,
    val deadline: LocalDate?,
    val targets: List<FinancialGoalTargetAmount>,
)

interface FinancialGoalManagementService {
    suspend fun listIndividualGoals(
        userId: UUID,
        pageable: Pageable,
    ): Page<FinancialGoalHeader>

    suspend fun listGroupGoals(
        userId: UUID,
        groupId: UUID,
        pageable: Pageable,
    ): Page<FinancialGoalHeader>

    suspend fun getGoalDetail(
        userId: UUID,
        goalId: UUID,
    ): FinancialGoalDetail

    suspend fun createGoal(
        userId: UUID,
        input: NewFinancialGoalInput,
    ): FinancialGoalHeader

    suspend fun createGoalForGroup(
        userId: UUID,
        groupId: UUID,
        input: NewFinancialGoalInput,
    ): FinancialGoalHeader

    suspend fun updateGoal(
        userId: UUID,
        goalId: UUID,
        input: EditFinancialGoalInput,
    ): FinancialGoalHeader

    suspend fun deleteGoal(
        userId: UUID,
        goalId: UUID,
    )

    suspend fun allocateImmediate(
        userId: UUID,
        goalId: UUID,
        walletItemId: UUID,
        amount: BigDecimal,
        allocationDate: LocalDate,
        note: String?,
    )

    suspend fun reverseAllocation(
        userId: UUID,
        goalId: UUID,
        walletItemId: UUID,
        amount: BigDecimal,
        note: String?,
    )

    suspend fun getLedgerMovement(
        userId: UUID,
        goalId: UUID,
        movementId: UUID,
    ): GoalLedgerMovementLine

    suspend fun listLedgerMovements(
        userId: UUID,
        goalId: UUID,
        pageable: Pageable,
    ): Page<GoalLedgerMovementLine>

    suspend fun editLedgerMovement(
        userId: UUID,
        goalId: UUID,
        movementId: UUID,
        newSignedAmount: BigDecimal,
        allocationDate: LocalDate?,
        note: String?,
    )

    suspend fun deleteLedgerMovement(
        userId: UUID,
        goalId: UUID,
        movementId: UUID,
    )

    suspend fun createSchedule(
        userId: UUID,
        goalId: UUID,
        walletItemId: UUID,
        amount: BigDecimal,
        periodicity: RecurrenceType,
        firstExecution: LocalDate,
        qtyLimit: Int?,
        removesAllocation: Boolean,
    ): GoalContributionScheduleLine

    suspend fun getSchedule(
        userId: UUID,
        goalId: UUID,
        scheduleId: UUID,
    ): GoalContributionScheduleLine

    suspend fun listSchedules(
        userId: UUID,
        goalId: UUID,
        pageable: Pageable,
    ): Page<GoalContributionScheduleLine>

    suspend fun updateSchedule(
        userId: UUID,
        goalId: UUID,
        scheduleId: UUID,
        walletItemId: UUID,
        amount: BigDecimal,
        periodicity: RecurrenceType,
        nextExecution: LocalDate,
        qtyLimit: Int?,
        removesAllocation: Boolean,
    ): GoalContributionScheduleLine

    suspend fun deleteSchedule(
        userId: UUID,
        goalId: UUID,
        scheduleId: UUID,
    )

    suspend fun materializeDueSchedules()
}
