package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.domain.models.bankaccount.NewBankAccountRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest
import com.ynixt.sharedfinances.scenarios.support.ScenarioRuntime
import com.ynixt.sharedfinances.scenarios.support.util.toBigDecimalSafe
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class WalletScenarioSetupOps(
    private val runtime: ScenarioRuntime,
    private val context: WalletScenarioContext,
    private val resolver: WalletScenarioResolver,
) {
    val currentCurrency: String
        get() = context.currentCurrency

    suspend fun createBankAccount(
        name: String = "Bank Account",
        balance: Number = BigDecimal.ZERO,
        currency: String = context.currentCurrency,
        showOnDashboard: Boolean = true,
    ): UUID {
        val userId = resolver.ensureUser()
        val account =
            runtime.bankAccountService.newBankAccount(
                userId = userId,
                newBankAccountRequest =
                    NewBankAccountRequest(
                        name = name,
                        balance = balance.toBigDecimalSafe(),
                        currency = currency,
                        showOnDashboard = showOnDashboard,
                    ),
            )
        return requireNotNull(account.id).also { id ->
            context.currentBankAccountId = id
        }
    }

    suspend fun createCreditCard(
        limit: Number,
        name: String = "Credit Card",
        currency: String = context.currentCurrency,
        dueDay: Int = 10,
        daysBetweenDueAndClosing: Int = 7,
        dueOnNextBusinessDay: Boolean = true,
        showOnDashboard: Boolean = true,
    ): UUID {
        val userId = resolver.ensureUser()
        val card =
            runtime.creditCardService.create(
                userId = userId,
                request =
                    NewCreditCardRequest(
                        name = name,
                        currency = currency,
                        totalLimit = limit.toBigDecimalSafe(),
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                        showOnDashboard = showOnDashboard,
                    ),
            )
        return requireNotNull(card.id).also { id ->
            context.currentCreditCardId = id
        }
    }

    suspend fun createCreditCardBill(
        billDate: LocalDate? = null,
        startValue: Number = BigDecimal.ZERO,
        creditCardId: UUID? = null,
    ) {
        val card = resolver.getCreditCard(creditCardId ?: resolver.requireCurrentCreditCardId())
        val effectiveBillDate = billDate ?: card.getBestBill(runtime.clock.today())
        val dueDate = card.getDueDate(effectiveBillDate)
        val closingDate = card.getClosingDate(dueDate)

        val bill =
            runtime.creditCardBillService.getOrCreateBill(
                creditCardId = requireNotNull(card.id),
                dueDate = dueDate,
                closingDate = closingDate,
                startValue = startValue.toBigDecimalSafe(),
            )

        context.lastBillId = requireNotNull(bill.id)
    }

    fun storeExchangeRateQuote(
        baseCurrency: String,
        quoteCurrency: String,
        quoteDate: LocalDate,
        rate: Number,
        source: String = "scenario-test",
    ) {
        runtime.storeExchangeRateQuote(
            baseCurrency = baseCurrency,
            quoteCurrency = quoteCurrency,
            quoteDate = quoteDate,
            rate = rate.toBigDecimalSafe(),
            source = source,
        )
    }
}
