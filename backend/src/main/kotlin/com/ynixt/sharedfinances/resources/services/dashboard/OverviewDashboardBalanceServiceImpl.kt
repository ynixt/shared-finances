package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
internal class OverviewDashboardBalanceServiceImpl {
    internal fun calculateBalanceForMonth(
        bankAccount: BankAccount,
        month: YearMonth,
        currentMonth: YearMonth,
        executedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
        projectedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
    ): BigDecimal {
        val bankId = bankAccount.id!!
        var result = bankAccount.balance

        if (month.isAfter(currentMonth)) {
            var cursor = currentMonth
            while (cursor.isBefore(month)) {
                result = result.add(projectedByMonthByBankId.getMonthAmount(cursor, bankId).net)
                cursor = cursor.plusMonths(1)
            }
            return result
        }

        var cursor = currentMonth
        while (cursor.isAfter(month)) {
            result = result.subtract(executedByMonthByBankId.getMonthAmount(cursor, bankId).net)
            cursor = cursor.minusMonths(1)
        }

        return result
    }

    internal fun calculateBalanceChartComponentsForMonth(
        bankAccount: BankAccount,
        month: YearMonth,
        currentMonth: YearMonth,
        executedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
        projectedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
    ): ChartValueComponents {
        if (month.isBefore(currentMonth)) {
            return ChartValueComponents(
                executed =
                    calculateBalanceForMonth(
                        bankAccount = bankAccount,
                        month = month,
                        currentMonth = currentMonth,
                        executedByMonthByBankId = executedByMonthByBankId,
                        projectedByMonthByBankId = projectedByMonthByBankId,
                    ).asMoney(),
                projected = BigDecimal.ZERO,
            )
        }

        return ChartValueComponents(
            executed = bankAccount.balance.asMoney(),
            projected =
                calculateProjectedBankNetThroughMonth(
                    bankAccount = bankAccount,
                    month = month,
                    currentMonth = currentMonth,
                    projectedByMonthByBankId = projectedByMonthByBankId,
                ).asMoney(),
        )
    }

    internal fun buildProjectedCreditCardBalanceChartContributions(
        chartMonths: List<YearMonth>,
        currentMonth: YearMonth,
        projectedCreditCardDetailsByMonth: Map<YearMonth, List<ProjectedCreditCardExpense>>,
    ): List<RawChartContribution> {
        if (projectedCreditCardDetailsByMonth.isEmpty()) {
            return emptyList()
        }

        val cumulativeByCardId = mutableMapOf<UUID, CreditCardProjectedBalanceAdjustment>()
        val contributions = mutableListOf<RawChartContribution>()

        chartMonths.forEach { month ->
            if (month.isBefore(currentMonth)) {
                return@forEach
            }

            projectedCreditCardDetailsByMonth[month].orEmpty().forEach { projectedExpense ->
                val current = cumulativeByCardId[projectedExpense.creditCardId]
                cumulativeByCardId[projectedExpense.creditCardId] =
                    if (current == null) {
                        CreditCardProjectedBalanceAdjustment(
                            currency = projectedExpense.currency,
                            amount = projectedExpense.projectedExpense,
                        )
                    } else {
                        current.copy(amount = current.amount.add(projectedExpense.projectedExpense))
                    }
            }

            cumulativeByCardId.values
                .filter { it.amount.compareTo(BigDecimal.ZERO) > 0 }
                .forEach { adjustment ->
                    contributions.add(
                        RawChartContribution(
                            chartSeries = ChartSeries.BALANCE,
                            component = ChartPointComponent.PROJECTED,
                            month = month,
                            value = adjustment.amount.negate(),
                            currency = adjustment.currency,
                            referenceDate = month.atEndOfMonth(),
                        ),
                    )
                }
        }

        return contributions
    }

    internal fun balanceReferenceDateForMonth(
        month: YearMonth,
        currentMonth: YearMonth,
        today: LocalDate,
    ): LocalDate =
        when {
            month.isBefore(currentMonth) -> month.atEndOfMonth()
            month == currentMonth -> today
            else -> month.atDay(1)
        }

    private fun calculateProjectedBankNetThroughMonth(
        bankAccount: BankAccount,
        month: YearMonth,
        currentMonth: YearMonth,
        projectedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
    ): BigDecimal {
        if (month.isBefore(currentMonth)) {
            return BigDecimal.ZERO
        }

        val bankId = bankAccount.id!!
        var cursor = currentMonth
        var result = BigDecimal.ZERO

        while (!cursor.isAfter(month)) {
            result = result.add(projectedByMonthByBankId.getMonthAmount(cursor, bankId).net)
            cursor = cursor.plusMonths(1)
        }

        return result
    }
}
