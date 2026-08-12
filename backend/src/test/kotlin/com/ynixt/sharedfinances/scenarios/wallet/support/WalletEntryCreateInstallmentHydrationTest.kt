package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.scenarios.support.ScenarioRuntime
import com.ynixt.sharedfinances.scenarios.user.support.UserScenarioSetupOps
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class WalletEntryCreateInstallmentHydrationTest {
    @Test
    fun `real create service hydrates recurrence metadata on an immediate installment`() =
        runTest {
            val today = LocalDate.of(2026, 8, 11)
            val runtime = ScenarioRuntime(initialDate = today)
            val context = WalletScenarioContext()
            lateinit var userSetup: UserScenarioSetupOps
            val resolver = WalletScenarioResolver(runtime, context) { userSetup.createUser(defaultCurrency = "BRL") }
            userSetup = UserScenarioSetupOps(runtime, context)
            val walletSetup = WalletScenarioSetupOps(runtime, context, resolver)
            val userId = userSetup.createUser(defaultCurrency = "BRL")
            val accountId = walletSetup.createBankAccount(name = "Checking", currency = "BRL")

            val created =
                runtime.walletEntryCreateService.create(
                    userId,
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = accountId,
                        date = today,
                        value = BigDecimal("25.00"),
                        name = "Imported installment",
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 1,
                        periodicity = RecurrenceType.MONTHLY,
                        seriesOffset = 2,
                        seriesQtyTotal = 12,
                    ),
                )

            val event = assertIs<WalletEventEntity>(created)
            val recurrence = assertNotNull(event.recurrenceEvent)
            assertEquals(event.recurrenceEventId, recurrence.id)
            assertEquals(2, recurrence.seriesOffset)
            assertEquals(12, recurrence.seriesQtyTotal)
        }
}
