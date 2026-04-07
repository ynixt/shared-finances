package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.scenarios.support.ScenarioRuntime
import com.ynixt.sharedfinances.scenarios.support.util.toBigDecimalSafe
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

class WalletScenarioWhen internal constructor(
    private val runtime: ScenarioRuntime,
    private val context: WalletScenarioContext,
    private val resolver: WalletScenarioResolver,
) {
    suspend fun revenue(
        value: Number,
        originId: UUID? = null,
        date: LocalDate,
        name: String = "Revenue",
        confirmed: Boolean = true,
    ) {
        val userId = resolver.ensureUser()
        val resolvedOrigin = resolver.resolveExpenseOrigin(originId = originId, date = date)

        val created =
            runtime.walletEntryCreateService.create(
                userId = userId,
                newEntryRequest =
                    NewEntryRequest(
                        type = WalletEntryType.REVENUE,
                        originId = resolvedOrigin.originId,
                        date = date,
                        value = value.toBigDecimalSafe(),
                        name = name,
                        confirmed = confirmed,
                        paymentType = PaymentType.UNIQUE,
                        originBillDate = resolvedOrigin.billDate,
                    ),
            )

        context.lastRecurrenceConfigId = resolver.extractRecurrenceId(created)
    }

    suspend fun expense(
        value: Number,
        originId: UUID? = null,
        date: LocalDate,
        name: String = "Expense",
        confirmed: Boolean = true,
        billDate: LocalDate? = null,
    ) {
        val userId = resolver.ensureUser()
        val resolvedOrigin = resolver.resolveExpenseOrigin(originId = originId, date = date, billDate = billDate)

        runtime.walletEntryCreateService.create(
            userId = userId,
            newEntryRequest =
                NewEntryRequest(
                    type = WalletEntryType.EXPENSE,
                    originId = resolvedOrigin.originId,
                    date = date,
                    value = value.toBigDecimalSafe(),
                    name = name,
                    confirmed = confirmed,
                    paymentType = PaymentType.UNIQUE,
                    originBillDate = resolvedOrigin.billDate,
                ),
        )
    }

    suspend fun installmentPurchase(
        total: Number,
        installments: Int,
        originId: UUID? = null,
        date: LocalDate,
        name: String = "Installment purchase",
    ) {
        require(installments > 1) { "Installments should be greater than 1" }

        val userId = resolver.ensureUser()
        val card = resolver.resolveCreditCard(originId)
        val billDate = card.getBestBill(date)
        val installmentValue =
            total
                .toBigDecimalSafe()
                .divide(installments.toBigDecimal(), 2, RoundingMode.HALF_UP)

        val created =
            runtime.walletEntryCreateService.create(
                userId = userId,
                newEntryRequest =
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = requireNotNull(card.id),
                        date = date,
                        value = installmentValue,
                        name = name,
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = installments,
                        periodicity = RecurrenceType.MONTHLY,
                        originBillDate = billDate,
                    ),
            )

        context.lastRecurrenceConfigId = resolver.extractRecurrenceId(created)
    }

    fun advanceTime(to: LocalDate) {
        runtime.clock.setDate(to)
    }

    suspend fun advanceTimeToNextRecurrenceExecution(recurrenceConfigId: UUID? = null) {
        runtime.clock.setDate(resolver.getNextRecurrenceExecutionDate(recurrenceConfigId))
    }

    suspend fun runRecurrence() {
        runtime.recurrenceService.queueAllPendingOfExecution()

        while (true) {
            val queued = runtime.queueProducer.poll() ?: break
            runtime.walletEntryCreateService.createFromRecurrenceConfig(
                recurrenceConfigId = queued.entryRecurrenceConfigId,
                date = queued.date,
            )
            context.lastRecurrenceConfigId = queued.entryRecurrenceConfigId
        }
    }
}
