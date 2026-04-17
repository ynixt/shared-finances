package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.services.dashboard.OverviewDashboardService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
internal class OverviewDashboardServiceImpl(
    private val dataService: OverviewDashboardDataServiceImpl,
    private val balanceService: OverviewDashboardBalanceServiceImpl,
    private val contributionService: OverviewDashboardContributionServiceImpl,
    private val goalService: OverviewDashboardGoalServiceImpl,
    private val assemblyService: OverviewDashboardAssemblyServiceImpl,
    private val cardService: OverviewDashboardCardServiceImpl,
    private val chartService: OverviewDashboardChartServiceImpl,
    private val clock: Clock,
) : OverviewDashboardService {
    override suspend fun getOverview(
        userId: UUID,
        defaultCurrency: String,
        selectedMonth: YearMonth,
    ): OverviewDashboard {
        val targetCurrency = defaultCurrency.uppercase()
        val today = LocalDate.now(clock)
        val currentMonth = YearMonth.from(today)
        val selectedMonthStart = selectedMonth.atDay(1)
        val selectedMonthEnd = selectedMonth.atEndOfMonth()
        val chartMonths = buildMonthRange(selectedMonth.minusMonths(11), selectedMonth)
        val maximumExecutedDate = minOf(today, selectedMonthEnd)

        val visibleItems = dataService.loadVisibleItems(userId)
        val executedByMonthByBankId =
            dataService.fetchExecutedByMonthByBank(
                userId = userId,
                minimumDate = chartMonths.first().atDay(1),
                maximumDate = maximumExecutedDate,
            )
        val projectedByMonthByBankId =
            dataService.fetchProjectedByMonthByBank(
                userId = userId,
                minimumDate = today.plusDays(1),
                maximumDate = selectedMonthEnd,
                visibleBankAccountIds = visibleItems.bankAccountIds,
            )
        val projectedCreditCardDetailsByMonth =
            dataService.fetchProjectedCreditCardCashOutByMonth(
                userId = userId,
                minimumMonth = currentMonth,
                maximumMonth = selectedMonth,
                creditCards = visibleItems.creditCards,
            )
        val projectedExpenseContributions =
            dataService.fetchProjectedExpenseContributions(
                userId = userId,
                minimumDate = maxOf(today.plusDays(1), chartMonths.first().atDay(1)),
                maximumDate = selectedMonthEnd,
                selectedMonth = selectedMonth,
                visibleWalletItemIds = visibleItems.walletItemIds,
            )
        val projectedCashBreakdownContributions =
            dataService.fetchProjectedCashBreakdownContributions(
                userId = userId,
                minimumDate = maxOf(today.plusDays(1), selectedMonthStart),
                maximumDate = selectedMonthEnd,
                selectedMonth = selectedMonth,
                visibleBankAccountIds = visibleItems.bankAccountIds,
            )
        val executedExpenseSourceSummaries =
            dataService.fetchExecutedExpenseBySource(
                userId = userId,
                minimumDate = selectedMonthStart,
                maximumDate = maximumExecutedDate,
            )
        val rawExpenseChartContributions =
            dataService.fetchExecutedExpenseChartContributions(
                userId = userId,
                minimumDate = chartMonths.first().atDay(1),
                maximumDate = maximumExecutedDate,
            ) + projectedExpenseContributions.chartContributions
        val rawBreakdownContributions =
            dataService.fetchExecutedCashBreakdownContributions(
                userId = userId,
                minimumDate = selectedMonthStart,
                maximumDate = selectedMonthEnd,
                referenceDate = selectedMonthEnd,
            ) +
                projectedCashBreakdownContributions +
                dataService.fetchExecutedExpenseBreakdownContributions(
                    userId = userId,
                    minimumDate = selectedMonthStart,
                    maximumDate = selectedMonthEnd,
                    referenceDate = selectedMonthEnd,
                ) +
                projectedExpenseContributions.breakdownContributions

        val rawDetailByCardKey =
            contributionService.buildRawDetailByCardKey(
                selectedMonth = selectedMonth,
                selectedMonthEnd = selectedMonthEnd,
                currentMonth = currentMonth,
                today = today,
                visibleBankAccounts = visibleItems.bankAccounts,
                executedByMonthByBankId = executedByMonthByBankId,
                projectedByMonthByBankId = projectedByMonthByBankId,
                projectedCreditCardDetails = projectedCreditCardDetailsByMonth[selectedMonth].orEmpty(),
                executedExpenseSourceSummaries = executedExpenseSourceSummaries,
                projectedExpenseDetails = projectedExpenseContributions.selectedMonthDetails,
            )
        val rawChartContributions =
            contributionService.buildRawChartContributions(
                chartMonths = chartMonths,
                currentMonth = currentMonth,
                today = today,
                visibleBankAccounts = visibleItems.bankAccounts,
                executedByMonthByBankId = executedByMonthByBankId,
                projectedByMonthByBankId = projectedByMonthByBankId,
                projectedCreditCardDetailsByMonth = projectedCreditCardDetailsByMonth,
                rawExpenseChartContributions = rawExpenseChartContributions,
            )

        val goalCommitmentReferenceDate = balanceService.balanceReferenceDateForMonth(selectedMonth, currentMonth, today)
        val rawGoalCommittedDetails =
            goalService.loadCommittedRawDetails(
                userId = userId,
                bankAccountIds = visibleItems.bankAccountIds,
                referenceDate = goalCommitmentReferenceDate,
            )
        val rawBalanceByBankId =
            contributionService.buildRawBalanceByBankId(
                rawDetailByCardKey[OverviewDashboardCardKey.BALANCE].orEmpty(),
            )
        val hasAccountOverCommittedBalance =
            goalService.hasAccountOverCommittedBalance(
                userId = userId,
                bankAccountIds = visibleItems.bankAccountIds,
                bankAccountById = visibleItems.bankAccountById,
                rawBalanceByBankId = rawBalanceByBankId,
            )

        val convertedValueByKey =
            assemblyService.convertRawValues(
                rawValues =
                    contributionService.collectRawValues(
                        rawDetailByCardKey = rawDetailByCardKey,
                        rawChartContributions = rawChartContributions,
                        rawGoalCommittedDetails = rawGoalCommittedDetails,
                        rawBreakdownContributions = rawBreakdownContributions,
                    ),
                targetCurrency = targetCurrency,
            )
        val convertedDetailByCardKey = assemblyService.buildConvertedDetails(rawDetailByCardKey, convertedValueByKey)

        val balanceTotal = assemblyService.sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.BALANCE])
        val goalCommittedTotal =
            rawGoalCommittedDetails
                .fold(BigDecimal.ZERO) { acc, raw -> acc.add(convertedValueByKey.getOrDefault(raw.key, BigDecimal.ZERO)) }
                .asMoney()
        val goalCommittedDetails =
            goalService.buildGoalCommittedDetailsByWallet(
                rawGoalCommittedDetails = rawGoalCommittedDetails,
                convertedValueByKey = convertedValueByKey,
                visibleBankAccounts = visibleItems.bankAccounts,
                rawBalanceByBankId = rawBalanceByBankId,
            )
        val freeBalanceTotal = balanceTotal.subtract(goalCommittedTotal).asMoney()
        val freeBalanceDetails =
            goalService.buildFreeBalanceDetailsByWallet(
                balanceTotal = balanceTotal,
                balanceDetails = convertedDetailByCardKey[OverviewDashboardCardKey.BALANCE].orEmpty(),
                rawGoalCommittedDetails = rawGoalCommittedDetails,
                convertedValueByKey = convertedValueByKey,
                visibleBankAccounts = visibleItems.bankAccounts,
                rawBalanceByBankId = rawBalanceByBankId,
            )
        val goalOverCommittedWarning = goalCommittedTotal.compareTo(balanceTotal) > 0 || hasAccountOverCommittedBalance

        val periodCashInTotal = assemblyService.sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_IN])
        val periodCashOutTotal = assemblyService.sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_OUT])
        val projectedCashInTotal = assemblyService.sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_IN])
        val projectedCashOutTotal = assemblyService.sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_OUT])
        val expensesTotal = assemblyService.sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.EXPENSES])
        val projectedExpensesTotal = assemblyService.sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_EXPENSES])
        val periodExpensesTotal = (expensesTotal + projectedExpensesTotal).asMoney()

        val periodNetCashFlowTotal = (periodCashInTotal - periodCashOutTotal).asMoney()
        val projectedNetCashFlowTotal = (projectedCashInTotal - projectedCashOutTotal).asMoney()
        val endOfPeriodBalanceTotal = (balanceTotal + projectedNetCashFlowTotal).asMoney()
        val endOfPeriodNetCashFlowTotal = (endOfPeriodBalanceTotal - balanceTotal).asMoney()

        val chartValuesBySeriesAndMonth = chartService.accumulateChartValues(rawChartContributions, convertedValueByKey)
        val breakdownValueByKey = chartService.accumulateBreakdownValues(rawBreakdownContributions, convertedValueByKey)

        val expenseByGroup =
            chartService.buildPieSlices(
                breakdownValueByKey = breakdownValueByKey,
                breakdownType = BreakdownType.EXPENSE_GROUP,
                alwaysIncludeLabel = PREDEFINED_INDIVIDUAL_LABEL,
            )
        val expenseByCategory =
            chartService.buildPieSlices(
                breakdownValueByKey = breakdownValueByKey,
                breakdownType = BreakdownType.EXPENSE_CATEGORY,
                alwaysIncludeLabel = PREDEFINED_UNCATEGORIZED_LABEL,
            )
        val cashInByCategory = chartService.buildPieSlices(breakdownValueByKey, BreakdownType.CASH_IN_CATEGORY)
        val cashOutByCategory = chartService.buildPieSlices(breakdownValueByKey, BreakdownType.CASH_OUT_CATEGORY)

        return OverviewDashboard(
            selectedMonth = selectedMonth,
            currency = targetCurrency,
            cards =
                cardService.buildCards(
                    convertedDetailByCardKey = convertedDetailByCardKey,
                    balanceTotal = balanceTotal,
                    goalCommittedTotal = goalCommittedTotal,
                    goalCommittedDetails = goalCommittedDetails,
                    freeBalanceTotal = freeBalanceTotal,
                    freeBalanceDetails = freeBalanceDetails,
                    periodCashInTotal = periodCashInTotal,
                    periodCashOutTotal = periodCashOutTotal,
                    periodNetCashFlowTotal = periodNetCashFlowTotal,
                    projectedCashInTotal = projectedCashInTotal,
                    projectedCashOutTotal = projectedCashOutTotal,
                    endOfPeriodNetCashFlowTotal = endOfPeriodNetCashFlowTotal,
                    expensesTotal = expensesTotal,
                    projectedExpensesTotal = projectedExpensesTotal,
                    periodExpensesTotal = periodExpensesTotal,
                    endOfPeriodBalanceTotal = endOfPeriodBalanceTotal,
                ),
            charts =
                chartService.buildCharts(
                    chartMonths = chartMonths,
                    chartValuesBySeriesAndMonth = chartValuesBySeriesAndMonth,
                    cashInByCategory = cashInByCategory,
                    cashOutByCategory = cashOutByCategory,
                    expenseByGroup = expenseByGroup,
                    expenseByCategory = expenseByCategory,
                ),
            goalCommittedTotal = goalCommittedTotal,
            freeBalanceTotal = freeBalanceTotal,
            goalOverCommittedWarning = goalOverCommittedWarning,
        )
    }
}
