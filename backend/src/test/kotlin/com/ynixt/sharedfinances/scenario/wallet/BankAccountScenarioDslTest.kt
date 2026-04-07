package com.ynixt.sharedfinances.scenario.wallet

import com.ynixt.sharedfinances.scenario.wallet.support.walletScenario
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
}
