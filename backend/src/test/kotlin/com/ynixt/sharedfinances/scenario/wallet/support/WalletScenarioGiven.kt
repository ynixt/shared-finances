package com.ynixt.sharedfinances.scenario.wallet.support

import com.ynixt.sharedfinances.scenario.user.support.UserScenarioSetupOps
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
    ) = walletSetupOps.createBankAccount(
        name = name,
        balance = balance,
        currency = currency,
    )

    suspend fun creditCard(
        limit: Number,
        name: String = "Credit Card",
        currency: String = walletSetupOps.currentCurrency,
        dueDay: Int = 10,
        daysBetweenDueAndClosing: Int = 7,
        dueOnNextBusinessDay: Boolean = true,
    ) = walletSetupOps.createCreditCard(
        limit = limit,
        name = name,
        currency = currency,
        dueDay = dueDay,
        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
        dueOnNextBusinessDay = dueOnNextBusinessDay,
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
}
