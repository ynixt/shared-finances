package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class CreditCardScenarioDslTest {
    @Test
    fun `should create credit card expense in 3 installments consuming total limit and generate only 3 bills`() {
        val today = LocalDate.of(2026, 1, 8)
        val totalLimit = BigDecimal("3000.00")
        val totalPurchaseValue = BigDecimal("300.00")
        val installmentValue = BigDecimal("100.00")
        val expectedAvailableLimit = BigDecimal("2700.00")
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false

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
        val billDate1 = expectedCardModel.getBestBill(today)
        val billDate2 = billDate1.plusMonths(1)
        val billDate3 = billDate1.plusMonths(2)
        val billDate4 = billDate1.plusMonths(3)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCard(
                    limit = totalLimit,
                    currency = "BRL",
                    dueDay = dueDay,
                    daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                    dueOnNextBusinessDay = dueOnNextBusinessDay,
                )
            }

            `when` {
                installmentPurchase(total = totalPurchaseValue, installments = 3, date = today)
            }

            then {
                billValueShouldBe(expected = installmentValue.unaryMinus(), billDate = billDate1)
                availableLimitShouldBe(expected = expectedAvailableLimit)
                recurrenceExecutionCountShouldBe(expected = 1)
                recurrenceLimitShouldBe(expected = 3)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                billValueShouldBe(expected = installmentValue.unaryMinus(), billDate = billDate2)
                availableLimitShouldBe(expected = expectedAvailableLimit)
                recurrenceExecutionCountShouldBe(expected = 2)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                billValueShouldBe(expected = installmentValue.unaryMinus(), billDate = billDate3)
                availableLimitShouldBe(expected = expectedAvailableLimit)
                recurrenceExecutionCountShouldBe(expected = 3)
                recurrenceNextExecutionShouldBe(expected = null)
            }

            `when` {
                advanceTime(to = billDate4)
                runRecurrence()
            }

            then {
                billShouldNotExist(billDate = billDate4)
                availableLimitShouldBe(expected = expectedAvailableLimit)
                recurrenceExecutionCountShouldBe(expected = 3)
                recurrenceNextExecutionShouldBe(expected = null)
            }
        }
    }

    @Test
    fun `should create missing bill when creating expense entry for current date on credit card`() {
        val today = LocalDate.of(2026, 1, 8)
        val totalLimit = BigDecimal("3000.00")
        val expenseValue = BigDecimal("150.00")
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false

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
        val expectedDueDate = expectedCardModel.getDueDate(expectedBillDate)
        val expectedClosingDate = expectedCardModel.getClosingDate(expectedDueDate)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCard(
                    limit = totalLimit,
                    currency = "BRL",
                    dueDay = dueDay,
                    daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                    dueOnNextBusinessDay = dueOnNextBusinessDay,
                )
            }

            then {
                billShouldNotExist(billDate = expectedBillDate)
            }

            `when` {
                expense(
                    value = expenseValue,
                    date = today,
                    name = "Farmacia",
                    billDate = expectedBillDate,
                )
            }

            then {
                billShouldExist(billDate = expectedBillDate)
                billDueDateShouldBe(expected = expectedDueDate, billDate = expectedBillDate)
                billClosingDateShouldBe(expected = expectedClosingDate, billDate = expectedBillDate)
                billValueShouldBe(expected = expenseValue.unaryMinus(), billDate = expectedBillDate)
                availableLimitShouldBe(expected = totalLimit.subtract(expenseValue))
            }
        }
    }
}
