package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.domain.enums.PlanningSimulationOutcomeBand
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningCurrencyMonthResult
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningTimelineMonthResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

internal data class PlanningTimelineEvaluation(
    val timeline: List<PlanningTimelineMonthResult>,
    val baselineFits: Boolean,
    val scheduledGoalTrackFits: Boolean,
)

internal object PlanningSimulationMath {
    fun classify(
        baselineFits: Boolean,
        scheduledGoalTrackFits: Boolean,
        counterfactualBaselineFits: Boolean,
    ): PlanningSimulationOutcomeBand =
        when {
            baselineFits && scheduledGoalTrackFits -> PlanningSimulationOutcomeBand.FITS
            baselineFits -> PlanningSimulationOutcomeBand.FITS_BUT_CANNOT_SUSTAIN_SCHEDULED_GOAL_CONTRIBUTIONS
            counterfactualBaselineFits -> PlanningSimulationOutcomeBand.FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED
            else -> PlanningSimulationOutcomeBand.DOES_NOT_FIT
        }

    fun evaluateTimeline(
        months: List<YearMonth>,
        currencies: Set<String>,
        openingByCurrency: Map<String, BigDecimal>,
        projectedByMonthCurrency: Map<Pair<YearMonth, String>, BigDecimal>,
        creditCardBillOutflowByMonthCurrency: Map<Pair<YearMonth, String>, BigDecimal>,
        debtOutflowByMonthCurrency: Map<Pair<YearMonth, String>, BigDecimal>,
        scheduledGoalContributionByMonthCurrency: Map<Pair<YearMonth, String>, BigDecimal>,
        openingBoostByCurrency: Map<String, BigDecimal>,
    ): PlanningTimelineEvaluation {
        val balanceByCurrency = mutableMapOf<String, BigDecimal>()
        val goalTrackBalanceByCurrency = mutableMapOf<String, BigDecimal>()

        currencies.forEach { currency ->
            val opening =
                openingByCurrency
                    .getOrDefault(
                        currency,
                        BigDecimal.ZERO,
                    ).add(openingBoostByCurrency.getOrDefault(currency, BigDecimal.ZERO))
            balanceByCurrency[currency] = asMoney(opening)
            goalTrackBalanceByCurrency[currency] = asMoney(opening)
        }

        val timeline = mutableListOf<PlanningTimelineMonthResult>()
        var baselineFits = true
        var scheduledGoalTrackFits = true

        months.forEach { month ->
            val byCurrency = linkedMapOf<String, PlanningCurrencyMonthResult>()

            currencies.forEach { currency ->
                val opening = asMoney(balanceByCurrency.getOrDefault(currency, BigDecimal.ZERO))
                val openingGoalTrack = asMoney(goalTrackBalanceByCurrency.getOrDefault(currency, BigDecimal.ZERO))
                val projected = asMoney(projectedByMonthCurrency.getOrDefault(month to currency, BigDecimal.ZERO))
                val billOutflow = asMoney(creditCardBillOutflowByMonthCurrency.getOrDefault(month to currency, BigDecimal.ZERO))
                val debtOutflow = asMoney(debtOutflowByMonthCurrency.getOrDefault(month to currency, BigDecimal.ZERO))
                val goalContribution = asMoney(scheduledGoalContributionByMonthCurrency.getOrDefault(month to currency, BigDecimal.ZERO))

                val closing = asMoney(opening.add(projected).subtract(billOutflow).subtract(debtOutflow))
                val closingWithGoals =
                    asMoney(
                        openingGoalTrack
                            .add(projected)
                            .subtract(billOutflow)
                            .subtract(debtOutflow)
                            .subtract(goalContribution),
                    )

                if (closing < BigDecimal.ZERO) {
                    baselineFits = false
                }
                if (closingWithGoals < BigDecimal.ZERO) {
                    scheduledGoalTrackFits = false
                }

                balanceByCurrency[currency] = closing
                goalTrackBalanceByCurrency[currency] = closingWithGoals

                byCurrency[currency] =
                    PlanningCurrencyMonthResult(
                        openingBalance = opening,
                        projectedCashFlow = projected,
                        creditCardBillOutflow = billOutflow,
                        debtOutflow = debtOutflow,
                        closingBalance = closing,
                        scheduledGoalContribution = goalContribution,
                        closingBalanceWithGoalContributions = closingWithGoals,
                    )
            }

            timeline +=
                PlanningTimelineMonthResult(
                    month = month.toString(),
                    byCurrency = byCurrency,
                )
        }

        return PlanningTimelineEvaluation(
            timeline = timeline,
            baselineFits = baselineFits,
            scheduledGoalTrackFits = scheduledGoalTrackFits,
        )
    }

    fun splitInstallments(
        total: BigDecimal,
        installments: Int,
    ): List<BigDecimal> {
        if (installments <= 1) {
            return listOf(asMoney(total))
        }

        val base =
            total
                .divide(BigDecimal.valueOf(installments.toLong()), 2, RoundingMode.HALF_UP)
                .let(::asMoney)
        val result = mutableListOf<BigDecimal>()
        var allocated = BigDecimal.ZERO

        repeat(installments) { index ->
            val value =
                if (index == installments - 1) {
                    asMoney(total.subtract(allocated))
                } else {
                    base
                }
            allocated = asMoney(allocated.add(value))
            result += value
        }

        return result
    }

    fun buildMonthRange(
        start: YearMonth,
        months: Int,
    ): List<YearMonth> {
        val result = mutableListOf<YearMonth>()
        var cursor = start
        repeat(months) {
            result += cursor
            cursor = cursor.plusMonths(1)
        }
        return result
    }

    fun asMoney(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}
