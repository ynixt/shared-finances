package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class TransferScenarioDslTest {
    @Test
    fun `should create transfer from bank account to credit card for current date and update balance and bill`() {
        val today = LocalDate.of(2026, 1, 8)
        val initialBalance = BigDecimal("1000.00")
        val transferValue = BigDecimal("250.00")
        val expectedBalance = initialBalance.subtract(transferValue)
        val totalLimit = BigDecimal("3000.00")
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false

        lateinit var bankAccountId: UUID
        lateinit var creditCardId: UUID

        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )
        val expectedBillDate = expectedCardModel.getBestBill(today)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId =
                    bankAccount(
                        name = "Main Account",
                        balance = initialBalance,
                        currency = "BRL",
                    )
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                transfer(
                    value = transferValue,
                    date = today,
                    name = "Bill Payment",
                    confirmed = true,
                    originId = bankAccountId,
                    targetId = creditCardId,
                )
            }

            then {
                balanceShouldBe(expected = expectedBalance)
                billValueShouldBe(expected = transferValue, billDate = expectedBillDate)
            }
        }
    }

    @Test
    fun `should create transfer from credit card to bank account for current date and update bill and balance`() {
        val today = LocalDate.of(2026, 1, 8)
        val initialBankBalance = BigDecimal("400.00")
        val transferValue = BigDecimal("120.00")
        val expectedBankBalance = initialBankBalance.add(transferValue)
        val totalLimit = BigDecimal("3000.00")
        val expectedAvailableLimit = totalLimit.subtract(transferValue)
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false

        lateinit var bankAccountId: UUID
        lateinit var creditCardId: UUID

        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )
        val expectedBillDate = expectedCardModel.getBestBill(today)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId =
                    bankAccount(
                        name = "Main Account",
                        balance = initialBankBalance,
                        currency = "BRL",
                    )
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                transfer(
                    value = transferValue,
                    date = today,
                    originId = creditCardId,
                    targetId = bankAccountId,
                    name = "Card Cash Withdrawal",
                    confirmed = true,
                )
            }

            then {
                availableLimitShouldBe(expected = expectedAvailableLimit, creditCardId = creditCardId)
                billValueShouldBe(expected = transferValue.unaryMinus(), billDate = expectedBillDate, creditCardId = creditCardId)
                balanceShouldBe(expected = expectedBankBalance, bankAccountId = bankAccountId)
            }
        }
    }
}
