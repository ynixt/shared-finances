package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.exceptions.http.TransferTargetValueRequiredException
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

class TransferScenarioDslTest {
    @Test
    fun `should mirror origin amount when same-currency transfer omits target amount`() {
        val today = LocalDate.of(2026, 1, 8)
        val originInitialBalance = BigDecimal("1000.00")
        val targetInitialBalance = BigDecimal("250.00")
        val originValue = BigDecimal("200.00")

        lateinit var originBankAccountId: UUID
        lateinit var targetBankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                originBankAccountId =
                    bankAccount(
                        name = "Origin Account",
                        balance = originInitialBalance,
                        currency = "BRL",
                    )
                targetBankAccountId =
                    bankAccount(
                        name = "Target Account",
                        balance = targetInitialBalance,
                        currency = "BRL",
                    )
            }

            `when` {
                transfer(
                    value = originValue,
                    date = today,
                    originId = originBankAccountId,
                    targetId = targetBankAccountId,
                    name = "Same Currency Mirror",
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(expected = originInitialBalance.subtract(originValue), bankAccountId = originBankAccountId)
                balanceShouldBe(expected = targetInitialBalance.add(originValue), bankAccountId = targetBankAccountId)
            }
        }
    }

    @Test
    fun `should require target amount for unique cross-currency transfer when omitted`() {
        val today = LocalDate.of(2026, 1, 8)

        assertThatThrownBy {
            lateinit var originBankAccountId: UUID
            lateinit var targetBankAccountId: UUID

            walletScenario(initialDate = today) {
                given {
                    user(defaultCurrency = "USD")
                    originBankAccountId =
                        bankAccount(
                            name = "US Account",
                            balance = BigDecimal("1000.00"),
                            currency = "USD",
                        )
                    targetBankAccountId =
                        bankAccount(
                            name = "Brazil Account",
                            balance = BigDecimal("5000.00"),
                            currency = "BRL",
                        )
                }

                `when` {
                    transfer(
                        value = BigDecimal("100.00"),
                        date = today,
                        originId = originBankAccountId,
                        targetId = targetBankAccountId,
                        name = "Missing target value",
                        confirmed = true,
                    )
                }
            }
        }.isInstanceOf(TransferTargetValueRequiredException::class.java)
    }

    @Test
    fun `should persist distinct origin and target amounts for same-currency transfer`() {
        val today = LocalDate.of(2026, 1, 8)
        val originInitialBalance = BigDecimal("1000.00")
        val targetInitialBalance = BigDecimal("250.00")
        val originValue = BigDecimal("200.00")
        val targetValue = BigDecimal("193.50")

        lateinit var originBankAccountId: UUID
        lateinit var targetBankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                originBankAccountId =
                    bankAccount(
                        name = "Origin Account",
                        balance = originInitialBalance,
                        currency = "BRL",
                    )
                targetBankAccountId =
                    bankAccount(
                        name = "Target Account",
                        balance = targetInitialBalance,
                        currency = "BRL",
                    )
            }

            `when` {
                transfer(
                    value = originValue,
                    targetValue = targetValue,
                    date = today,
                    originId = originBankAccountId,
                    targetId = targetBankAccountId,
                    name = "Same Currency With Fees",
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(expected = originInitialBalance.subtract(originValue), bankAccountId = originBankAccountId)
                balanceShouldBe(expected = targetInitialBalance.add(targetValue), bankAccountId = targetBankAccountId)
            }
        }
    }

    @Test
    fun `should persist distinct origin and target amounts for cross-currency transfer`() {
        val today = LocalDate.of(2026, 1, 8)
        val originInitialBalance = BigDecimal("1000.00")
        val targetInitialBalance = BigDecimal("5000.00")
        val originValue = BigDecimal("100.00")
        val targetValue = BigDecimal("540.25")

        lateinit var originBankAccountId: UUID
        lateinit var targetBankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "USD")
                originBankAccountId =
                    bankAccount(
                        name = "US Account",
                        balance = originInitialBalance,
                        currency = "USD",
                    )
                targetBankAccountId =
                    bankAccount(
                        name = "Brazil Account",
                        balance = targetInitialBalance,
                        currency = "BRL",
                    )
            }

            `when` {
                transfer(
                    value = originValue,
                    targetValue = targetValue,
                    date = today,
                    originId = originBankAccountId,
                    targetId = targetBankAccountId,
                    name = "USD to BRL",
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(expected = originInitialBalance.subtract(originValue), bankAccountId = originBankAccountId)
                balanceShouldBe(expected = targetInitialBalance.add(targetValue), bankAccountId = targetBankAccountId)
            }
        }
    }

    @Test
    fun `should hide target value for recurring transfer template and resolve it on occurrence creation`() {
        val today = LocalDate.of(2026, 1, 8)
        val scheduledExecutionDate = LocalDate.of(2026, 1, 10)
        val originInitialBalance = BigDecimal("1000.00")
        val targetInitialBalance = BigDecimal("5000.00")
        val originValue = BigDecimal("100.00")
        val resolvedTargetValue = BigDecimal("540.00")

        lateinit var originBankAccountId: UUID
        lateinit var targetBankAccountId: UUID

        walletScenario(
            initialDate = today,
            exchangeRateService = fixedExchangeRateService(rate = BigDecimal("5.40")),
        ) {
            given {
                user(defaultCurrency = "USD")
                originBankAccountId =
                    bankAccount(
                        name = "US Account",
                        balance = originInitialBalance,
                        currency = "USD",
                    )
                targetBankAccountId =
                    bankAccount(
                        name = "Brazil Account",
                        balance = targetInitialBalance,
                        currency = "BRL",
                    )
            }

            `when` {
                transfer(
                    value = originValue,
                    date = scheduledExecutionDate,
                    originId = originBankAccountId,
                    targetId = targetBankAccountId,
                    name = "Recurring USD to BRL",
                    confirmed = true,
                    paymentType = com.ynixt.sharedfinances.domain.enums.PaymentType.RECURRING,
                    periodicity = com.ynixt.sharedfinances.domain.enums.RecurrenceType.MONTHLY,
                    periodicityQtyLimit = 2,
                )
                fetchFirstScheduledExecution()
            }

            then {
                fetchedScheduledWalletEventShouldExist()
                fetchedScheduledWalletEventShouldHideTargetValue()
                balanceShouldBe(expected = originInitialBalance, bankAccountId = originBankAccountId)
                balanceShouldBe(expected = targetInitialBalance, bankAccountId = targetBankAccountId)
            }

            `when` {
                advanceTime(scheduledExecutionDate)
                runRecurrence()
            }

            then {
                balanceShouldBe(expected = originInitialBalance.subtract(originValue), bankAccountId = originBankAccountId)
                balanceShouldBe(expected = targetInitialBalance.add(resolvedTargetValue), bankAccountId = targetBankAccountId)
            }
        }
    }

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

    private fun fixedExchangeRateService(rate: BigDecimal): ExchangeRateService =
        object : ExchangeRateService {
            override suspend fun syncLatestQuotes(): Int = 0

            override suspend fun syncQuotesForDate(
                date: LocalDate,
                baseCurrencies: Set<String>?,
            ): Int = 0

            override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity> =
                CursorPage(
                    items = emptyList(),
                    nextCursor = null,
                    hasNext = false,
                )

            override suspend fun getRate(
                fromCurrency: String,
                toCurrency: String,
                referenceDate: LocalDate,
            ): BigDecimal = rate

            override suspend fun resolveRate(
                fromCurrency: String,
                toCurrency: String,
                referenceDate: LocalDate,
            ): ResolvedExchangeRate = ResolvedExchangeRate(rate = rate, quoteDate = referenceDate)

            override suspend fun convert(
                value: BigDecimal,
                fromCurrency: String,
                toCurrency: String,
                referenceDate: LocalDate,
            ): BigDecimal = value.multiply(rate).setScale(2, RoundingMode.HALF_UP)

            override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> =
                requests.associateWith { request ->
                    request.value.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                }
        }
}
