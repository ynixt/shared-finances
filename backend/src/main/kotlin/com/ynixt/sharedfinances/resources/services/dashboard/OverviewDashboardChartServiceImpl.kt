package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardChartPoint
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCharts
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardPieSlice
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
internal class OverviewDashboardChartServiceImpl {
    internal fun accumulateChartValues(
        rawChartContributions: List<RawChartContribution>,
        convertedValueByKey: Map<String, BigDecimal>,
    ): Map<Pair<ChartSeries, YearMonth>, ChartValueComponents> {
        val chartValuesBySeriesAndMonth = linkedMapOf<Pair<ChartSeries, YearMonth>, ChartValueComponents>()

        rawChartContributions.forEach { rawChartContribution ->
            val key = rawChartContribution.chartSeries to rawChartContribution.month
            val current = chartValuesBySeriesAndMonth.getOrDefault(key, ChartValueComponents.ZERO)
            val convertedValue = convertedValueByKey.getOrDefault(rawChartContribution.key, BigDecimal.ZERO)
            chartValuesBySeriesAndMonth[key] =
                when (rawChartContribution.component) {
                    ChartPointComponent.EXECUTED -> current.copy(executed = current.executed.add(convertedValue))
                    ChartPointComponent.PROJECTED -> current.copy(projected = current.projected.add(convertedValue))
                }
        }

        return chartValuesBySeriesAndMonth
    }

    internal fun accumulateBreakdownValues(
        rawBreakdownContributions: List<RawBreakdownContribution>,
        convertedValueByKey: Map<String, BigDecimal>,
    ): Map<BreakdownSliceKey, BreakdownValueComponents> {
        val breakdownValueByKey = linkedMapOf<BreakdownSliceKey, BreakdownValueComponents>()

        rawBreakdownContributions.forEach { rawBreakdownContribution ->
            val key =
                BreakdownSliceKey(
                    breakdownType = rawBreakdownContribution.breakdownType,
                    sliceId = rawBreakdownContribution.sliceId,
                    label = rawBreakdownContribution.label,
                )
            val current = breakdownValueByKey.getOrDefault(key, BreakdownValueComponents.ZERO)
            val convertedValue = convertedValueByKey.getOrDefault(rawBreakdownContribution.key, BigDecimal.ZERO)
            breakdownValueByKey[key] =
                when (rawBreakdownContribution.component) {
                    ChartPointComponent.EXECUTED -> current.copy(executed = current.executed.add(convertedValue))
                    ChartPointComponent.PROJECTED -> current.copy(projected = current.projected.add(convertedValue))
                }
        }

        return breakdownValueByKey
    }

    internal fun buildPieSlices(
        breakdownValueByKey: Map<BreakdownSliceKey, BreakdownValueComponents>,
        breakdownType: BreakdownType,
        alwaysIncludeLabel: String? = null,
    ): List<OverviewDashboardPieSlice> =
        finalizePieSlices(
            slices =
                breakdownValueByKey.entries
                    .filter { it.key.breakdownType == breakdownType }
                    .map { (key, components) ->
                        OverviewDashboardPieSlice(
                            id = key.sliceId,
                            label = key.label,
                            value = components.total().asMoney(),
                            executedValue = components.executed.asMoney(),
                            projectedValue = components.projected.asMoney(),
                        )
                    },
            alwaysIncludeLabel = alwaysIncludeLabel,
        )

    internal fun buildCharts(
        chartMonths: List<YearMonth>,
        chartValuesBySeriesAndMonth: Map<Pair<ChartSeries, YearMonth>, ChartValueComponents>,
        cashInByCategory: List<OverviewDashboardPieSlice>,
        cashOutByCategory: List<OverviewDashboardPieSlice>,
        expenseByGroup: List<OverviewDashboardPieSlice>,
        expenseByCategory: List<OverviewDashboardPieSlice>,
    ): OverviewDashboardCharts =
        OverviewDashboardCharts(
            balance = buildChartPoints(chartMonths, ChartSeries.BALANCE, chartValuesBySeriesAndMonth),
            cashIn = buildChartPoints(chartMonths, ChartSeries.CASH_IN, chartValuesBySeriesAndMonth),
            cashOut = buildChartPoints(chartMonths, ChartSeries.CASH_OUT, chartValuesBySeriesAndMonth),
            expense = buildChartPoints(chartMonths, ChartSeries.EXPENSE, chartValuesBySeriesAndMonth),
            cashInByCategory = cashInByCategory,
            cashOutByCategory = cashOutByCategory,
            expenseByGroup = expenseByGroup,
            expenseByCategory = expenseByCategory,
        )

    private fun finalizePieSlices(
        slices: List<OverviewDashboardPieSlice>,
        alwaysIncludeLabel: String? = null,
    ): List<OverviewDashboardPieSlice> {
        val sortedSlices =
            slices
                .filter { it.value.compareTo(BigDecimal.ZERO) > 0 }
                .sortedWith(compareByDescending<OverviewDashboardPieSlice> { it.value }.thenBy { it.label })

        if (sortedSlices.isEmpty()) {
            return emptyList()
        }

        val naturalNamed = sortedSlices.take(MAX_NAMED_BREAKDOWN_SLICES)
        val forcedSlice = alwaysIncludeLabel?.let { label -> sortedSlices.firstOrNull { it.label == label } }
        val namedSlices =
            if (forcedSlice == null || naturalNamed.any { it.sliceIdentity() == forcedSlice.sliceIdentity() }) {
                naturalNamed
            } else {
                (
                    sortedSlices.filterNot { it.sliceIdentity() == forcedSlice.sliceIdentity() }.take(MAX_NAMED_BREAKDOWN_SLICES - 1) +
                        forcedSlice
                ).sortedWith(compareByDescending<OverviewDashboardPieSlice> { it.value }.thenBy { it.label })
            }

        val namedIdentity = namedSlices.map { it.sliceIdentity() }.toSet()
        val otherSlices = sortedSlices.filterNot { namedIdentity.contains(it.sliceIdentity()) }
        val othersExecutedValue = otherSlices.fold(BigDecimal.ZERO) { acc, slice -> acc.add(slice.executedValue) }.asMoney()
        val othersProjectedValue = otherSlices.fold(BigDecimal.ZERO) { acc, slice -> acc.add(slice.projectedValue) }.asMoney()
        val othersValue = othersExecutedValue.add(othersProjectedValue).asMoney()

        if (othersValue.compareTo(BigDecimal.ZERO) <= 0) {
            return namedSlices
        }

        return namedSlices +
            OverviewDashboardPieSlice(
                id = null,
                label = PREDEFINED_OTHERS_LABEL,
                value = othersValue,
                executedValue = othersExecutedValue,
                projectedValue = othersProjectedValue,
            )
    }

    private fun buildChartPoints(
        chartMonths: List<YearMonth>,
        chartSeries: ChartSeries,
        chartValuesBySeriesAndMonth: Map<Pair<ChartSeries, YearMonth>, ChartValueComponents>,
    ): List<OverviewDashboardChartPoint> =
        chartMonths.map { month ->
            val components = chartValuesBySeriesAndMonth.getOrDefault(chartSeries to month, ChartValueComponents.ZERO)
            OverviewDashboardChartPoint(
                month = month,
                value = components.total().asMoney(),
                executedValue = components.executed.asMoney(),
                projectedValue = components.projected.asMoney(),
            )
        }
}
