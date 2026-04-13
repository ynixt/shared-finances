package com.ynixt.sharedfinances.scenarios.planning

import com.ynixt.sharedfinances.domain.enums.PlanningSimulationOutcomeBand
import com.ynixt.sharedfinances.scenarios.planning.support.planningScenario
import org.junit.jupiter.api.Test
import java.time.YearMonth

class PlanningScenarioDslTest {
    @Test
    fun `future inflow should not cover earlier shortfall`() {
        planningScenario(startMonth = YearMonth.of(2026, 1), horizonMonths = 2) {
            given {
                openingBalance(currency = "BRL", amount = 0)
                projectedCashFlow(month = YearMonth.of(2026, 1), currency = "BRL", amount = -100)
                projectedCashFlow(month = YearMonth.of(2026, 2), currency = "BRL", amount = 200)
            }

            `when` { runSimulation() }

            then {
                outcomeShouldBe(PlanningSimulationOutcomeBand.DOES_NOT_FIT)
                closingBalanceShouldBe(month = YearMonth.of(2026, 1), currency = "BRL", expected = -100)
                closingBalanceShouldBe(month = YearMonth.of(2026, 2), currency = "BRL", expected = 100)
            }
        }
    }

    @Test
    fun `scenario DSL should cover installments, goal reallocation band, group opt-in warning and aggregated projections`() {
        planningScenario(startMonth = YearMonth.of(2026, 3), horizonMonths = 3) {
            given {
                openingBalance(currency = "BRL", amount = 80)
                projectedCashFlow(month = YearMonth.of(2026, 3), currency = "BRL", amount = 10)
                projectedCashFlow(month = YearMonth.of(2026, 3), currency = "BRL", amount = 15) // multi-origin-like aggregate
                simulatedInstallmentDebt(total = 120, installments = 3, firstMonth = YearMonth.of(2026, 3), currency = "BRL")
                committedGoalAllocation(currency = "BRL", amount = 80)
                scheduledGoalContribution(month = YearMonth.of(2026, 4), currency = "BRL", amount = 20)
                groupOptIn(includedMembers = 1, excludedMembers = 1)
            }

            `when` { runSimulation() }

            then {
                outcomeShouldBe(PlanningSimulationOutcomeBand.FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED)
                shouldWarnIncompleteGroupSimulation(expected = true)
            }
        }
    }
}
