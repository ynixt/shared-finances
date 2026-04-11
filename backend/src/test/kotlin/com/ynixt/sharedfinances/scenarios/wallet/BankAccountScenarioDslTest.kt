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
