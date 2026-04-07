package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BankAccountScenarioDslTest {
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
                    name = "Conta Principal",
                    balance = initialBalance,
                    currency = "BRL",
                )
            }

            `when` {
                expense(
                    value = expenseValue,
                    date = today,
                    name = "Supermercado",
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
                    name = "Conta Principal",
                    balance = initialBalance,
                    currency = "BRL",
                )
            }

            `when` {
                revenue(
                    value = revenueValue,
                    date = futureDate,
                    name = "Lucro Futuro",
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
