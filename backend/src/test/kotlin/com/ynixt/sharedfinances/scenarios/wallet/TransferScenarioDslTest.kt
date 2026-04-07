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

    @Test
    fun `should create transfer between credit cards and use correct bill date for each card`() {
        val today = LocalDate.of(2026, 1, 8)
        val transferValue = BigDecimal("100.00")
        val originLimit = BigDecimal("3000.00")
        val targetLimit = BigDecimal("1500.00")
        val originDueDay = 8
        val originDaysBetweenDueAndClosing = 1
        val targetDueDay = 20
        val targetDaysBetweenDueAndClosing = 5
        val dueOnNextBusinessDay = false

        lateinit var originCreditCardId: UUID
        lateinit var targetCreditCardId: UUID

        val expectedOriginCardModel =
            CreditCard(
                name = "Expected Origin Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = originLimit,
                balance = originLimit,
                dueDay = originDueDay,
                daysBetweenDueAndClosing = originDaysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )
        val expectedTargetCardModel =
            CreditCard(
                name = "Expected Target Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = targetLimit,
                balance = targetLimit,
                dueDay = targetDueDay,
                daysBetweenDueAndClosing = targetDaysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )

        val expectedOriginBillDate = expectedOriginCardModel.getBestBill(today)
        val expectedTargetBillDate = expectedTargetCardModel.getBestBill(today)
        check(expectedOriginBillDate != expectedTargetBillDate) {
            "Expected different bill dates for origin and target cards"
        }

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                originCreditCardId =
                    creditCard(
                        name = "Origin Card",
                        limit = originLimit,
                        currency = "BRL",
                        dueDay = originDueDay,
                        daysBetweenDueAndClosing = originDaysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
                targetCreditCardId =
                    creditCard(
                        name = "Target Card",
                        limit = targetLimit,
                        currency = "BRL",
                        dueDay = targetDueDay,
                        daysBetweenDueAndClosing = targetDaysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                transfer(
                    value = transferValue,
                    date = today,
                    originId = originCreditCardId,
                    targetId = targetCreditCardId,
                    name = "Card to Card Transfer",
                    confirmed = true,
                )
            }

            then {
                billValueShouldBe(
                    expected = transferValue.unaryMinus(),
                    billDate = expectedOriginBillDate,
                    creditCardId = originCreditCardId,
                )
                billShouldNotExist(
                    billDate = expectedTargetBillDate,
                    creditCardId = originCreditCardId,
                )
                billValueShouldBe(
                    expected = transferValue,
                    billDate = expectedTargetBillDate,
                    creditCardId = targetCreditCardId,
                )
                billShouldNotExist(
                    billDate = expectedOriginBillDate,
                    creditCardId = targetCreditCardId,
                )
                availableLimitShouldBe(
                    expected = originLimit.subtract(transferValue),
                    creditCardId = originCreditCardId,
                )
                availableLimitShouldBe(
                    expected = targetLimit.add(transferValue),
                    creditCardId = targetCreditCardId,
                )
            }
        }
    }
}
