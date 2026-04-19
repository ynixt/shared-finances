package com.ynixt.sharedfinances.domain.models.simulation.planning

import com.ynixt.sharedfinances.domain.enums.PlanningSimulationOutcomeBand
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class PlanningSimulationRequest(
    val startDate: LocalDate? = null,
    val horizonMonths: Int? = null,
    val expenses: List<PlanningSimulatedExpenseRequest>? = null,
)

data class PlanningSimulatedExpenseRequest(
    val name: String? = null,
    val amount: BigDecimal,
    val firstPaymentDate: LocalDate? = null,
    val installments: Int? = null,
    val sourceWalletItemId: UUID? = null,
    val currency: String? = null,
)

enum class PlanningSimulationScopeType {
    USER,
    GROUP,
}

data class PlanningSimulationResult(
    val scopeType: PlanningSimulationScopeType,
    val outcomeBand: PlanningSimulationOutcomeBand,
    val timeline: List<PlanningTimelineMonthResult>,
    val goalTrack: PlanningGoalTrackResult,
    val groupContext: PlanningGroupContextResult? = null,
)

data class PlanningTimelineMonthResult(
    val month: String,
    val byCurrency: Map<String, PlanningCurrencyMonthResult>,
)

data class PlanningCurrencyMonthResult(
    val openingBalance: BigDecimal,
    val projectedCashFlow: BigDecimal,
    val creditCardBillOutflow: BigDecimal,
    val simulatedExpenseOutflow: BigDecimal,
    val debtOutflow: BigDecimal,
    val debtInflow: BigDecimal,
    val closingBalance: BigDecimal,
    val scheduledGoalContribution: BigDecimal,
    val closingBalanceWithGoalContributions: BigDecimal,
)

data class PlanningGoalTrackResult(
    val canSustainScheduledContributions: Boolean,
    val canSustainScheduledContributionsIfAllocationsAreFreed: Boolean,
    val canFitIfAllocationsAreFreed: Boolean,
    val committedAllocationsByCurrency: Map<String, BigDecimal>,
)

data class PlanningGroupContextResult(
    val incompleteSimulation: Boolean,
    val includedMembers: Int,
    val excludedMembers: Int,
    val privacyLabels: List<String>,
)
