package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.domain.enums.PlanningSimulationOutcomeBand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class PlanningSimulationMathTest {
    @Test
    fun `buildMonthRange should return sequential months`() {
        val months = PlanningSimulationMath.buildMonthRange(YearMonth.of(2026, 1), 4)

        assertThat(months)
            .containsExactly(
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2),
                YearMonth.of(2026, 3),
                YearMonth.of(2026, 4),
            )
    }

    @Test
    fun `splitInstallments should keep cents and remainder on last installment`() {
        val installments = PlanningSimulationMath.splitInstallments(BigDecimal("100.00"), 3)

        assertThat(installments)
            .containsExactly(
                BigDecimal("33.33"),
                BigDecimal("33.33"),
                BigDecimal("33.34"),
            )
    }

    @Test
    fun `evaluateTimeline should not allow future inflow to fix past shortfall`() {
        val months =
            listOf(
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2),
            )
        val evaluation =
            PlanningSimulationMath.evaluateTimeline(
                months = months,
                currencies = setOf("BRL"),
                openingByCurrency = mapOf("BRL" to BigDecimal.ZERO),
                projectedByMonthCurrency =
                    mapOf(
                        YearMonth.of(2026, 1) to "BRL" to BigDecimal("-100.00"),
                        YearMonth.of(2026, 2) to "BRL" to BigDecimal("200.00"),
                    ),
                creditCardBillOutflowByMonthCurrency = emptyMap(),
                debtOutflowByMonthCurrency = emptyMap(),
                scheduledGoalContributionByMonthCurrency = emptyMap(),
                openingBoostByCurrency = emptyMap(),
            )

        assertThat(evaluation.baselineFits).isFalse()
        assertThat(
            evaluation.timeline
                .first()
                .byCurrency["BRL"]!!
                .closingBalance,
        ).isEqualByComparingTo(BigDecimal("-100.00"))
        assertThat(
            evaluation.timeline
                .last()
                .byCurrency["BRL"]!!
                .closingBalance,
        ).isEqualByComparingTo(BigDecimal("100.00"))
    }

    @Test
    fun `evaluateTimeline should keep goal contributions in separate track`() {
        val month = YearMonth.of(2026, 1)
        val evaluation =
            PlanningSimulationMath.evaluateTimeline(
                months = listOf(month),
                currencies = setOf("BRL"),
                openingByCurrency = mapOf("BRL" to BigDecimal("100.00")),
                projectedByMonthCurrency = emptyMap(),
                creditCardBillOutflowByMonthCurrency = emptyMap(),
                debtOutflowByMonthCurrency = emptyMap(),
                scheduledGoalContributionByMonthCurrency = mapOf(month to "BRL" to BigDecimal("150.00")),
                openingBoostByCurrency = emptyMap(),
            )

        assertThat(evaluation.baselineFits).isTrue()
        assertThat(evaluation.scheduledGoalTrackFits).isFalse()
    }

    @Test
    fun `classify should map each outcome band`() {
        assertThat(
            PlanningSimulationMath.classify(
                baselineFits = true,
                scheduledGoalTrackFits = true,
                counterfactualBaselineFits = true,
            ),
        ).isEqualTo(PlanningSimulationOutcomeBand.FITS)

        assertThat(
            PlanningSimulationMath.classify(
                baselineFits = true,
                scheduledGoalTrackFits = false,
                counterfactualBaselineFits = true,
            ),
        ).isEqualTo(PlanningSimulationOutcomeBand.FITS_BUT_CANNOT_SUSTAIN_SCHEDULED_GOAL_CONTRIBUTIONS)

        assertThat(
            PlanningSimulationMath.classify(
                baselineFits = false,
                scheduledGoalTrackFits = false,
                counterfactualBaselineFits = true,
            ),
        ).isEqualTo(PlanningSimulationOutcomeBand.FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED)

        assertThat(
            PlanningSimulationMath.classify(
                baselineFits = false,
                scheduledGoalTrackFits = false,
                counterfactualBaselineFits = false,
            ),
        ).isEqualTo(PlanningSimulationOutcomeBand.DOES_NOT_FIT)
    }
}
