package com.ynixt.sharedfinances.scenarios.planning.support

import com.ynixt.sharedfinances.domain.enums.PlanningSimulationOutcomeBand
import com.ynixt.sharedfinances.resources.services.simulation.PlanningSimulationMath
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.YearMonth

internal fun planningScenario(
    startMonth: YearMonth = YearMonth.of(2026, 1),
    horizonMonths: Int = 6,
    block: suspend PlanningScenarioDsl.() -> Unit,
): PlanningScenarioDsl =
    runBlocking {
        PlanningScenarioDsl(startMonth, horizonMonths).apply { block() }
    }

internal class PlanningScenarioDsl(
    private val startMonth: YearMonth,
    private val horizonMonths: Int,
) {
    private val openingByCurrency = linkedMapOf<String, BigDecimal>()
    private val projectedByMonthCurrency = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()
    private val goalContributionByMonthCurrency = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()
    private val committedByCurrency = linkedMapOf<String, BigDecimal>()
    private val simulatedExpenseOutflowByMonthCurrency = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()
    private val debtOutflowByMonthCurrency = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()
    private val debtInflowByMonthCurrency = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()
    private var includedMembers = 1
    private var excludedMembers = 0

    private var lastRun: PlanningScenarioResult? = null

    val given = PlanningScenarioGiven(this)
    val whenActions = PlanningScenarioWhen(this)
    val then = PlanningScenarioThen(this)

    suspend fun given(block: suspend PlanningScenarioGiven.() -> Unit): PlanningScenarioDsl {
        given.block()
        return this
    }

    suspend fun `when`(block: suspend PlanningScenarioWhen.() -> Unit): PlanningScenarioDsl {
        whenActions.block()
        return this
    }

    suspend fun then(block: suspend PlanningScenarioThen.() -> Unit): PlanningScenarioDsl {
        then.block()
        return this
    }

    internal fun opening(
        currency: String,
        amount: BigDecimal,
    ) {
        openingByCurrency[currency.uppercase()] = amount.setScale(2)
    }

    internal fun projected(
        month: YearMonth,
        currency: String,
        amount: BigDecimal,
    ) {
        val key = month to currency.uppercase()
        projectedByMonthCurrency[key] = projectedByMonthCurrency.getOrDefault(key, BigDecimal.ZERO).add(amount).setScale(2)
    }

    internal fun goalContribution(
        month: YearMonth,
        currency: String,
        amount: BigDecimal,
    ) {
        val key = month to currency.uppercase()
        goalContributionByMonthCurrency[key] =
            goalContributionByMonthCurrency.getOrDefault(key, BigDecimal.ZERO).add(amount).setScale(2)
    }

    internal fun committed(
        currency: String,
        amount: BigDecimal,
    ) {
        committedByCurrency[currency.uppercase()] = amount.setScale(2)
    }

    internal fun installmentExpense(
        total: BigDecimal,
        installments: Int,
        firstMonth: YearMonth,
        currency: String,
    ) {
        PlanningSimulationMath.splitInstallments(total, installments).forEachIndexed { index, value ->
            val month = firstMonth.plusMonths(index.toLong())
            val key = month to currency.uppercase()
            simulatedExpenseOutflowByMonthCurrency[key] =
                simulatedExpenseOutflowByMonthCurrency.getOrDefault(key, BigDecimal.ZERO).add(value).setScale(2)
        }
    }

    internal fun debtOutflow(
        month: YearMonth,
        currency: String,
        amount: BigDecimal,
    ) {
        val key = month to currency.uppercase()
        debtOutflowByMonthCurrency[key] =
            debtOutflowByMonthCurrency.getOrDefault(key, BigDecimal.ZERO).add(amount).setScale(2)
    }

    internal fun debtInflow(
        month: YearMonth,
        currency: String,
        amount: BigDecimal,
    ) {
        val key = month to currency.uppercase()
        debtInflowByMonthCurrency[key] =
            debtInflowByMonthCurrency.getOrDefault(key, BigDecimal.ZERO).add(amount).setScale(2)
    }

    internal fun groupMembers(
        included: Int,
        excluded: Int,
    ) {
        includedMembers = included
        excludedMembers = excluded
    }

    internal fun execute() {
        val months = PlanningSimulationMath.buildMonthRange(startMonth, horizonMonths)
        val currencies =
            (
                openingByCurrency.keys +
                    projectedByMonthCurrency.keys.map { it.second } +
                    goalContributionByMonthCurrency.keys.map { it.second } +
                    simulatedExpenseOutflowByMonthCurrency.keys.map { it.second } +
                    debtOutflowByMonthCurrency.keys.map { it.second } +
                    debtInflowByMonthCurrency.keys.map { it.second } +
                    committedByCurrency.keys
            ).toSet()

        val baseline =
            PlanningSimulationMath.evaluateTimeline(
                months = months,
                currencies = currencies,
                openingByCurrency = openingByCurrency,
                projectedByMonthCurrency = projectedByMonthCurrency,
                creditCardBillOutflowByMonthCurrency = emptyMap(),
                simulatedExpenseOutflowByMonthCurrency = simulatedExpenseOutflowByMonthCurrency,
                debtOutflowByMonthCurrency = debtOutflowByMonthCurrency,
                debtInflowByMonthCurrency = debtInflowByMonthCurrency,
                scheduledGoalContributionByMonthCurrency = goalContributionByMonthCurrency,
                openingBoostByCurrency = emptyMap(),
            )
        val counterfactual =
            PlanningSimulationMath.evaluateTimeline(
                months = months,
                currencies = currencies,
                openingByCurrency = openingByCurrency,
                projectedByMonthCurrency = projectedByMonthCurrency,
                creditCardBillOutflowByMonthCurrency = emptyMap(),
                simulatedExpenseOutflowByMonthCurrency = simulatedExpenseOutflowByMonthCurrency,
                debtOutflowByMonthCurrency = debtOutflowByMonthCurrency,
                debtInflowByMonthCurrency = debtInflowByMonthCurrency,
                scheduledGoalContributionByMonthCurrency = goalContributionByMonthCurrency,
                openingBoostByCurrency = committedByCurrency,
            )

        lastRun =
            PlanningScenarioResult(
                baseline = baseline,
                counterfactual = counterfactual,
                outcomeBand =
                    PlanningSimulationMath.classify(
                        baselineFits = baseline.baselineFits,
                        scheduledGoalTrackFits = baseline.scheduledGoalTrackFits,
                        counterfactualBaselineFits = counterfactual.baselineFits,
                    ),
                incompleteGroupSimulation = excludedMembers > 0,
                includedMembers = includedMembers,
                excludedMembers = excludedMembers,
            )
    }

    internal fun result(): PlanningScenarioResult =
        requireNotNull(lastRun) { "Scenario was not executed. Call whenActions.runSimulation()" }
}

internal class PlanningScenarioGiven internal constructor(
    private val dsl: PlanningScenarioDsl,
) {
    fun openingBalance(
        currency: String,
        amount: Number,
    ) {
        dsl.opening(currency, amount.toBigDecimalSafe())
    }

    fun projectedCashFlow(
        month: YearMonth,
        currency: String,
        amount: Number,
    ) {
        dsl.projected(month, currency, amount.toBigDecimalSafe())
    }

    fun scheduledGoalContribution(
        month: YearMonth,
        currency: String,
        amount: Number,
    ) {
        dsl.goalContribution(month, currency, amount.toBigDecimalSafe())
    }

    fun committedGoalAllocation(
        currency: String,
        amount: Number,
    ) {
        dsl.committed(currency, amount.toBigDecimalSafe())
    }

    fun simulatedInstallmentExpense(
        total: Number,
        installments: Int,
        firstMonth: YearMonth,
        currency: String,
    ) {
        dsl.installmentExpense(total.toBigDecimalSafe(), installments, firstMonth, currency)
    }

    fun debtOutflow(
        month: YearMonth,
        currency: String,
        amount: Number,
    ) {
        dsl.debtOutflow(month, currency, amount.toBigDecimalSafe())
    }

    fun debtInflow(
        month: YearMonth,
        currency: String,
        amount: Number,
    ) {
        dsl.debtInflow(month, currency, amount.toBigDecimalSafe())
    }

    fun groupOptIn(
        includedMembers: Int,
        excludedMembers: Int,
    ) {
        dsl.groupMembers(included = includedMembers, excluded = excludedMembers)
    }
}

internal class PlanningScenarioWhen internal constructor(
    private val dsl: PlanningScenarioDsl,
) {
    fun runSimulation() {
        dsl.execute()
    }
}

internal class PlanningScenarioThen internal constructor(
    private val dsl: PlanningScenarioDsl,
) {
    fun outcomeShouldBe(expected: PlanningSimulationOutcomeBand) {
        val actual = dsl.result().outcomeBand
        org.assertj.core.api.Assertions
            .assertThat(actual)
            .isEqualTo(expected)
    }

    fun closingBalanceShouldBe(
        month: YearMonth,
        currency: String,
        expected: Number,
    ) {
        val timeline = dsl.result().baseline.timeline
        val point = timeline.first { it.month == month.toString() }
        val actual = point.byCurrency[currency.uppercase()]!!.closingBalance
        org.assertj.core.api.Assertions
            .assertThat(actual)
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    fun shouldWarnIncompleteGroupSimulation(expected: Boolean) {
        org.assertj.core.api.Assertions
            .assertThat(dsl.result().incompleteGroupSimulation)
            .isEqualTo(expected)
    }
}

internal data class PlanningScenarioResult(
    val baseline: com.ynixt.sharedfinances.resources.services.simulation.PlanningTimelineEvaluation,
    val counterfactual: com.ynixt.sharedfinances.resources.services.simulation.PlanningTimelineEvaluation,
    val outcomeBand: PlanningSimulationOutcomeBand,
    val incompleteGroupSimulation: Boolean,
    val includedMembers: Int,
    val excludedMembers: Int,
)

private fun Number.toBigDecimalSafe(): BigDecimal = BigDecimal(this.toString()).setScale(2)
