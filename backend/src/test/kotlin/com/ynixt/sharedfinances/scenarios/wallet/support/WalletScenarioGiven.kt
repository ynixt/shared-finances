package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.scenarios.user.support.UserScenarioSetupOps
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class WalletScenarioGiven internal constructor(
    private val userSetupOps: UserScenarioSetupOps,
    private val walletSetupOps: WalletScenarioSetupOps,
) {
    suspend fun user(
        email: String = "user-${UUID.randomUUID()}@example.com",
        password: String = "password123",
        firstName: String = "Scenario",
        lastName: String = "User",
        lang: String = "en",
        defaultCurrency: String = "USD",
        tmz: String = "UTC",
    ) = userSetupOps.createUser(
        email = email,
        password = password,
        firstName = firstName,
        lastName = lastName,
        lang = lang,
        defaultCurrency = defaultCurrency,
        tmz = tmz,
    )

    suspend fun bankAccount(
        name: String = "Bank Account",
        balance: Number = BigDecimal.ZERO,
        currency: String = walletSetupOps.currentCurrency,
        showOnDashboard: Boolean = true,
    ) = walletSetupOps.createBankAccount(
        name = name,
        balance = balance,
        currency = currency,
        showOnDashboard = showOnDashboard,
    )

    suspend fun creditCard(
        limit: Number,
        name: String = "Credit Card",
        currency: String = walletSetupOps.currentCurrency,
        dueDay: Int = 10,
        daysBetweenDueAndClosing: Int = 7,
        dueOnNextBusinessDay: Boolean = true,
        showOnDashboard: Boolean = true,
    ) = walletSetupOps.createCreditCard(
        limit = limit,
        name = name,
        currency = currency,
        dueDay = dueDay,
        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
        dueOnNextBusinessDay = dueOnNextBusinessDay,
        showOnDashboard = showOnDashboard,
    )

    suspend fun creditCardBill(
        billDate: LocalDate? = null,
        startValue: Number = BigDecimal.ZERO,
        creditCardId: UUID? = null,
    ) = walletSetupOps.createCreditCardBill(
        billDate = billDate,
        startValue = startValue,
        creditCardId = creditCardId,
    )

    fun exchangeRateQuote(
        baseCurrency: String,
        quoteCurrency: String,
        quoteDate: LocalDate,
        rate: Number,
        source: String = "scenario-test",
    ) = walletSetupOps.storeExchangeRateQuote(
        baseCurrency = baseCurrency,
        quoteCurrency = quoteCurrency,
        quoteDate = quoteDate,
        rate = rate,
        source = source,
    )
}
