package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BankAccountScenarioDslTest {
    @Test
    fun `should not count initial bank balance as overview cash in`() {
        val today = LocalDate.of(2026, 1, 8)
        val selectedMonth = YearMonth.from(today)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccount(
                    name = "Main Account",
                    balance = 1000,
                    currency = "BRL",
                )
            }

            `when` {
                fetchOverview(selectedMonth)
            }

            then {
                overviewCardShouldBe(OverviewDashboardCardKey.BALANCE, 1000)
                overviewCardShouldBe(OverviewDashboardCardKey.PERIOD_CASH_IN, 0)
                overviewCardShouldBe(OverviewDashboardCardKey.PERIOD_CASH_OUT, 0)
                overviewCardShouldBe(OverviewDashboardCardKey.PERIOD_NET_CASH_FLOW, 0)
            }
        }
    }

    @Test
    fun `should calculate formulas for selected month from executed and projected bank flows`() {
        val initialDate = LocalDate.of(2026, 4, 1)
        val selectedMonth = YearMonth.of(2026, 4)
        val executedRevenueDate = LocalDate.of(2026, 4, 10)
        val executedExpenseDate = LocalDate.of(2026, 4, 12)
        val today = LocalDate.of(2026, 4, 15)
        val projectedRevenueDate = LocalDate.of(2026, 4, 20)
        val projectedExpenseDate = LocalDate.of(2026, 4, 22)

        walletScenario(initialDate = initialDate) {
            given {
                user(defaultCurrency = "BRL")
                bankAccount(
                    name = "Main",
                    balance = 800,
                    currency = "BRL",
                )
            }

            `when` {
                advanceTime(to = executedRevenueDate)
                revenue(
                    value = 300,
                    date = executedRevenueDate,
                    name = "Executed revenue",
                    confirmed = true,
                )

                advanceTime(to = executedExpenseDate)
                expense(
                    value = 100,
                    date = executedExpenseDate,
                    name = "Executed expense",
                    confirmed = true,
                )

                advanceTime(to = today)
                revenue(
                    value = 100,
                    date = projectedRevenueDate,
                    name = "Projected revenue",
                    confirmed = true,
                )
                expense(
                    value = 50,
                    date = projectedExpenseDate,
                    name = "Projected expense",
                    confirmed = true,
                )
                fetchOverview(selectedMonth)
            }

            then {
                overviewCardShouldBe(OverviewDashboardCardKey.BALANCE, 1000)
                overviewCardShouldBe(OverviewDashboardCardKey.PERIOD_CASH_IN, 300)
                overviewCardShouldBe(OverviewDashboardCardKey.PERIOD_CASH_OUT, 100)
                overviewCardShouldBe(OverviewDashboardCardKey.PERIOD_NET_CASH_FLOW, 200)
                overviewCardShouldBe(OverviewDashboardCardKey.PROJECTED_CASH_IN, 100)
                overviewCardShouldBe(OverviewDashboardCardKey.PROJECTED_CASH_OUT, 50)
                overviewCardShouldBe(OverviewDashboardCardKey.END_OF_PERIOD_BALANCE, 1050)
                overviewCardShouldBe(OverviewDashboardCardKey.END_OF_PERIOD_NET_CASH_FLOW, 50)
                overviewChartBalanceForMonthShouldBe(selectedMonth, 1050)
            }
        }
    }

    @Test
    fun `should use future opening balance rule for selected overview month`() {
        val today = LocalDate.of(2026, 4, 15)
        val selectedMonth = YearMonth.of(2026, 6)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccount(
                    name = "Future account",
                    balance = 1000,
                    currency = "BRL",
                )
            }

            `when` {
                revenue(
                    value = 200,
                    date = LocalDate.of(2026, 4, 20),
                    name = "Projected April revenue",
                    confirmed = true,
                )
                revenue(
                    value = 300,
                    date = LocalDate.of(2026, 5, 10),
                    name = "Projected May revenue",
                    confirmed = true,
                )
                fetchOverview(selectedMonth)
            }

            then {
                overviewCardShouldBe(OverviewDashboardCardKey.BALANCE, 1500)
            }
        }
    }

    @Test
    fun `should create expense entry for current date and decrease bank account balance`() {
        val today = LocalDate.of(2026, 1, 8)
        val initialBalance = BigDecimal("1000.00")
        val expenseValue = BigDecimal("150.00")
        val expectedBalance = initialBalance.subtract(expenseValue)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccount(
                    name = "Main Account",
                    balance = initialBalance,
                    currency = "BRL",
                )
            }

            `when` {
                expense(
                    value = expenseValue,
                    date = today,
                    name = "Supermarket",
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(expected = expectedBalance)
            }
        }
    }

    @Test
    fun `should only apply future revenue to bank account balance after recurrence execution`() {
        val today = LocalDate.of(2026, 1, 8)
        val futureDate = today.plusDays(5)
        val initialBalance = BigDecimal("1000.00")
        val revenueValue = BigDecimal("200.00")
        val expectedBalanceAfterExecution = initialBalance.add(revenueValue)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccount(
                    name = "Main Account",
                    balance = initialBalance,
                    currency = "BRL",
                )
            }

            `when` {
                revenue(
                    value = revenueValue,
                    date = futureDate,
                    name = "Future Profit",
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(expected = initialBalance)
            }

            `when` {
                advanceTime(to = futureDate)
                runRecurrence()
            }

            then {
                balanceShouldBe(expected = expectedBalanceAfterExecution)
            }
        }
    }
}
