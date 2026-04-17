package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseSourceSummary
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
internal class OverviewDashboardContributionServiceImpl(
    private val balanceService: OverviewDashboardBalanceServiceImpl,
) {
    internal fun buildRawDetailByCardKey(
        selectedMonth: YearMonth,
        selectedMonthEnd: LocalDate,
        currentMonth: YearMonth,
        today: LocalDate,
        visibleBankAccounts: List<BankAccount>,
        executedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
        projectedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
        projectedCreditCardDetails: List<ProjectedCreditCardExpense>,
        executedExpenseSourceSummaries: List<OverviewExpenseSourceSummary>,
        projectedExpenseDetails: List<OverviewExpenseSourceSummary>,
    ): Map<OverviewDashboardCardKey, List<RawDetail>> {
        val rawDetailByCardKey = linkedMapOf<OverviewDashboardCardKey, MutableList<RawDetail>>()

        fun addRawDetail(
            cardKey: OverviewDashboardCardKey,
            detail: RawDetail,
        ) {
            rawDetailByCardKey.getOrPut(cardKey) { mutableListOf() }.add(detail)
        }

        visibleBankAccounts.forEach { bankAccount ->
            val bankId = bankAccount.id!!
            val selectedMonthBalance =
                balanceService.calculateBalanceForMonth(
                    bankAccount = bankAccount,
                    month = selectedMonth,
                    currentMonth = currentMonth,
                    executedByMonthByBankId = executedByMonthByBankId,
                    projectedByMonthByBankId = projectedByMonthByBankId,
                )
            val selectedMonthBalanceReferenceDate =
                balanceService.balanceReferenceDateForMonth(selectedMonth, currentMonth, today)

            val selectedExecuted = executedByMonthByBankId.getMonthAmount(selectedMonth, bankId)
            val selectedProjected = projectedByMonthByBankId.getMonthAmount(selectedMonth, bankId)

            addRawDetail(
                OverviewDashboardCardKey.BALANCE,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedMonthBalance,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthBalanceReferenceDate,
                ),
            )
            addRawDetail(
                OverviewDashboardCardKey.PERIOD_CASH_IN,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedExecuted.cashIn,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
            addRawDetail(
                OverviewDashboardCardKey.PERIOD_CASH_OUT,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedExecuted.cashOut,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
            addRawDetail(
                OverviewDashboardCardKey.PROJECTED_CASH_IN,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedProjected.cashIn,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
            addRawDetail(
                OverviewDashboardCardKey.PROJECTED_CASH_OUT,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedProjected.cashOut,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
        }

        projectedCreditCardDetails.forEach { creditCardProjected ->
            addRawDetail(
                OverviewDashboardCardKey.PROJECTED_CASH_OUT,
                RawDetail(
                    sourceId = creditCardProjected.creditCardId,
                    sourceType = OverviewDashboardDetailSourceType.CREDIT_CARD_BILL,
                    label = creditCardProjected.creditCardName,
                    value = creditCardProjected.projectedExpense,
                    currency = creditCardProjected.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
        }

        executedExpenseSourceSummaries.forEach { expenseSummary ->
            addRawDetail(
                OverviewDashboardCardKey.EXPENSES,
                RawDetail(
                    sourceId = expenseSummary.walletItemId,
                    sourceType = detailSourceTypeForWalletItemType(expenseSummary.walletItemType),
                    label = expenseSummary.walletItemName,
                    value = expenseSummary.expense,
                    currency = expenseSummary.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
        }

        projectedExpenseDetails.forEach { expenseSummary ->
            addRawDetail(
                OverviewDashboardCardKey.PROJECTED_EXPENSES,
                RawDetail(
                    sourceId = expenseSummary.walletItemId,
                    sourceType = detailSourceTypeForWalletItemType(expenseSummary.walletItemType),
                    label = expenseSummary.walletItemName,
                    value = expenseSummary.expense,
                    currency = expenseSummary.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
        }

        return rawDetailByCardKey
    }

    internal fun buildRawChartContributions(
        chartMonths: List<YearMonth>,
        currentMonth: YearMonth,
        today: LocalDate,
        visibleBankAccounts: List<BankAccount>,
        executedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
        projectedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
        projectedCreditCardDetailsByMonth: Map<YearMonth, List<ProjectedCreditCardExpense>>,
        rawExpenseChartContributions: List<RawChartContribution>,
    ): List<RawChartContribution> {
        val rawChartContributions = mutableListOf<RawChartContribution>()

        chartMonths.forEach { month ->
            val monthEnd = month.atEndOfMonth()

            visibleBankAccounts.forEach { bankAccount ->
                val bankId = bankAccount.id!!
                val executed = executedByMonthByBankId.getMonthAmount(month, bankId)
                val projected = projectedByMonthByBankId.getMonthAmount(month, bankId)
                val balanceComponents =
                    balanceService.calculateBalanceChartComponentsForMonth(
                        bankAccount = bankAccount,
                        month = month,
                        currentMonth = currentMonth,
                        executedByMonthByBankId = executedByMonthByBankId,
                        projectedByMonthByBankId = projectedByMonthByBankId,
                    )
                val balanceReferenceDate = balanceService.balanceReferenceDateForMonth(month, currentMonth, today)

                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.BALANCE,
                        component = ChartPointComponent.EXECUTED,
                        month = month,
                        value = balanceComponents.executed,
                        currency = bankAccount.currency,
                        referenceDate = balanceReferenceDate,
                    ),
                )
                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.BALANCE,
                        component = ChartPointComponent.PROJECTED,
                        month = month,
                        value = balanceComponents.projected,
                        currency = bankAccount.currency,
                        referenceDate = balanceReferenceDate,
                    ),
                )
                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.CASH_IN,
                        component = ChartPointComponent.EXECUTED,
                        month = month,
                        value = executed.cashIn,
                        currency = bankAccount.currency,
                        referenceDate = monthEnd,
                    ),
                )
                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.CASH_IN,
                        component = ChartPointComponent.PROJECTED,
                        month = month,
                        value = projected.cashIn,
                        currency = bankAccount.currency,
                        referenceDate = monthEnd,
                    ),
                )
                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.CASH_OUT,
                        component = ChartPointComponent.EXECUTED,
                        month = month,
                        value = executed.cashOut,
                        currency = bankAccount.currency,
                        referenceDate = monthEnd,
                    ),
                )
                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.CASH_OUT,
                        component = ChartPointComponent.PROJECTED,
                        month = month,
                        value = projected.cashOut,
                        currency = bankAccount.currency,
                        referenceDate = monthEnd,
                    ),
                )
            }

            projectedCreditCardDetailsByMonth[month].orEmpty().forEach { projectedExpense ->
                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.CASH_OUT,
                        component = ChartPointComponent.PROJECTED,
                        month = month,
                        value = projectedExpense.projectedExpense,
                        currency = projectedExpense.currency,
                        referenceDate = monthEnd,
                    ),
                )
            }
        }

        rawChartContributions.addAll(
            balanceService.buildProjectedCreditCardBalanceChartContributions(
                chartMonths = chartMonths,
                currentMonth = currentMonth,
                projectedCreditCardDetailsByMonth = projectedCreditCardDetailsByMonth,
            ),
        )
        rawChartContributions.addAll(rawExpenseChartContributions)

        return rawChartContributions
    }

    internal fun collectRawValues(
        rawDetailByCardKey: Map<OverviewDashboardCardKey, List<RawDetail>>,
        rawChartContributions: List<RawChartContribution>,
        rawGoalCommittedDetails: List<RawDetail>,
        rawBreakdownContributions: List<RawBreakdownContribution>,
    ): List<RawValue> =
        buildList {
            rawDetailByCardKey.values.flatten().forEach { rawDetail ->
                add(
                    RawValue(
                        key = rawDetail.key,
                        value = rawDetail.value,
                        currency = rawDetail.currency,
                        referenceDate = rawDetail.referenceDate,
                    ),
                )
            }
            rawChartContributions.forEach { rawChartContribution ->
                add(
                    RawValue(
                        key = rawChartContribution.key,
                        value = rawChartContribution.value,
                        currency = rawChartContribution.currency,
                        referenceDate = rawChartContribution.referenceDate,
                    ),
                )
            }
            rawGoalCommittedDetails.forEach { rawDetail ->
                add(
                    RawValue(
                        key = rawDetail.key,
                        value = rawDetail.value,
                        currency = rawDetail.currency,
                        referenceDate = rawDetail.referenceDate,
                    ),
                )
            }
            rawBreakdownContributions.forEach { rawBreakdownContribution ->
                add(
                    RawValue(
                        key = rawBreakdownContribution.key,
                        value = rawBreakdownContribution.value,
                        currency = rawBreakdownContribution.currency,
                        referenceDate = rawBreakdownContribution.referenceDate,
                    ),
                )
            }
        }

    internal fun buildRawBalanceByBankId(balanceDetails: List<RawDetail>): Map<UUID, BigDecimal> =
        balanceDetails
            .mapNotNull { detail ->
                val sourceId = detail.sourceId
                if (sourceId == null || detail.sourceType != OverviewDashboardDetailSourceType.BANK_ACCOUNT) {
                    null
                } else {
                    sourceId to detail.value.asMoney()
                }
            }.toMap()
}
