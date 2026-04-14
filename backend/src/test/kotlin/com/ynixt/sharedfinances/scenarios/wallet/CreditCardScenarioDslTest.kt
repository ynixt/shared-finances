package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
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
                    name = "Pharmacy",
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

    @Test
    fun `should keep dashboard expense semantics and breakdowns after partial bill payment`() {
        val initialDate = LocalDate.of(2026, 3, 31)
        val purchaseDate = LocalDate.of(2026, 4, 5)
        val paymentDate = LocalDate.of(2026, 4, 8)
        val selectedMonth = YearMonth.of(2026, 4)
        val billDate = LocalDate.of(2026, 4, 1)

        walletScenario(initialDate = initialDate) {
            given {
                user(defaultCurrency = "BRL")
                bankAccount(balance = 1000, currency = "BRL")
                creditCard(
                    limit = 1000,
                    currency = "BRL",
                    dueDay = 20,
                    daysBetweenDueAndClosing = 10,
                    dueOnNextBusinessDay = false,
                )
            }

            `when` {
                advanceTime(to = purchaseDate)
                expense(value = 200, date = purchaseDate, name = "Laptop stand")
                fetchOverview(selectedMonth)
            }

            then {
                overviewCardShouldBe(OverviewDashboardCardKey.PERIOD_CASH_OUT, 0)
                overviewCardShouldBe(OverviewDashboardCardKey.PROJECTED_CASH_OUT, 200)
                overviewExpenseForMonthShouldBe(selectedMonth, 200)
                overviewExpenseGroupSliceShouldBe("PREDEFINED_INDIVIDUAL", 200)
                overviewExpenseCategorySliceShouldBe("PREDEFINED_UNCATEGORIZED", 200)
            }

            `when` {
                advanceTime(to = paymentDate)
                payBill(amount = 50, date = paymentDate, billDate = billDate)
                fetchOverview(selectedMonth)
            }

            then {
                billValueShouldBe(expected = -150, billDate = billDate)
                balanceShouldBe(expected = 950)
                overviewCardShouldBe(OverviewDashboardCardKey.PERIOD_CASH_OUT, 50)
                overviewCardShouldBe(OverviewDashboardCardKey.PROJECTED_CASH_OUT, 150)
                overviewExpenseForMonthShouldBe(selectedMonth, 200)
                overviewExpenseGroupSliceShouldBe("PREDEFINED_INDIVIDUAL", 200)
                overviewExpenseCategorySliceShouldBe("PREDEFINED_UNCATEGORIZED", 200)
            }
        }
    }

    @Test
    fun `should include projected unpaid credit card bills and exclude hidden items from overview`() {
        val today = LocalDate.of(2026, 4, 15)
        val selectedMonth = YearMonth.of(2026, 4)
        val billDate = LocalDate.of(2026, 4, 1)

        lateinit var visibleBankId: UUID
        lateinit var hiddenBankId: UUID
        lateinit var visibleCardId: UUID
        lateinit var hiddenCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                visibleBankId =
                    bankAccount(
                        name = "Visible bank",
                        balance = 1000,
                        currency = "BRL",
                        showOnDashboard = true,
                    )
                hiddenBankId =
                    bankAccount(
                        name = "Hidden bank",
                        balance = 9999,
                        currency = "BRL",
                        showOnDashboard = false,
                    )

                visibleCardId =
                    creditCard(
                        limit = 1000,
                        name = "Visible card",
                        currency = "BRL",
                        dueDay = 10,
                        daysBetweenDueAndClosing = 7,
                        dueOnNextBusinessDay = false,
                        showOnDashboard = true,
                    )
                hiddenCardId =
                    creditCard(
                        limit = 1000,
                        name = "Hidden card",
                        currency = "BRL",
                        dueDay = 10,
                        daysBetweenDueAndClosing = 7,
                        dueOnNextBusinessDay = false,
                        showOnDashboard = false,
                    )

                creditCardBill(
                    billDate = billDate,
                    startValue = -400,
                    creditCardId = visibleCardId,
                )
                creditCardBill(
                    billDate = billDate,
                    startValue = -700,
                    creditCardId = hiddenCardId,
                )
            }

            `when` {
                expense(
                    originId = visibleBankId,
                    value = 100,
                    date = LocalDate.of(2026, 4, 20),
                    name = "Visible projected expense",
                    confirmed = true,
                )
                expense(
                    originId = hiddenBankId,
                    value = 500,
                    date = LocalDate.of(2026, 4, 21),
                    name = "Hidden projected expense",
                    confirmed = true,
                )
                fetchOverview(selectedMonth)
            }

            then {
                overviewCardShouldBe(OverviewDashboardCardKey.PROJECTED_CASH_OUT, 500)
                overviewCardShouldBe(OverviewDashboardCardKey.END_OF_PERIOD_NET_CASH_FLOW, -500)
                overviewCardDetailLabelsShouldContain(
                    key = OverviewDashboardCardKey.PROJECTED_CASH_OUT,
                    expectedLabels = listOf("Visible bank", "Visible card"),
                )
                overviewCardDetailLabelsShouldNotContain(
                    key = OverviewDashboardCardKey.PROJECTED_CASH_OUT,
                    unexpectedLabels = listOf("Hidden bank", "Hidden card"),
                )
            }
        }
    }
}
