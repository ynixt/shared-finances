package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.DeleteScheduledEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EditScheduledEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.scenarios.support.ScenarioRuntime
import com.ynixt.sharedfinances.scenarios.support.util.toBigDecimalSafe
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class WalletScenarioWhen internal constructor(
    private val runtime: ScenarioRuntime,
    private val context: WalletScenarioContext,
    private val resolver: WalletScenarioResolver,
) {
    suspend fun createEntry(newEntryRequest: NewEntryRequest) {
        val userId = resolver.ensureUser()
        val created =
            runtime.walletEntryCreateService.create(
                userId = userId,
                newEntryRequest = newEntryRequest,
            )

        context.lastRecurrenceConfigId = resolver.extractRecurrenceId(created)
        context.lastWalletEventId = resolver.extractWalletEventId(created)
    }

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
        context.lastWalletEventId = resolver.extractWalletEventId(created)
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

        val created =
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

        context.lastWalletEventId = resolver.extractWalletEventId(created)
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
        context.lastWalletEventId = resolver.extractWalletEventId(created)
    }

    suspend fun transfer(
        value: Number,
        targetValue: Number? = null,
        date: LocalDate,
        groupId: UUID? = null,
        originId: UUID,
        targetId: UUID,
        name: String = "Transfer",
        confirmed: Boolean = true,
        paymentType: PaymentType = PaymentType.UNIQUE,
        installments: Int? = null,
        periodicity: RecurrenceType = RecurrenceType.SINGLE,
        periodicityQtyLimit: Int? = null,
        originBillDate: LocalDate? = null,
        targetBillDate: LocalDate? = null,
    ) {
        val userId = resolver.ensureUser()

        require(originId != targetId) {
            "Origin and target should be different for transfer"
        }

        val origin =
            requireNotNull(runtime.walletItemService.findOne(originId)) {
                "Wallet item $originId was not found"
            }
        val target =
            requireNotNull(runtime.walletItemService.findOne(targetId)) {
                "Wallet item $targetId was not found"
            }

        val resolvedOriginBillDate =
            if (origin is CreditCard) {
                originBillDate ?: origin.getBestBill(date)
            } else {
                null
            }
        val resolvedTargetBillDate =
            if (target is CreditCard) {
                targetBillDate ?: target.getBestBill(date)
            } else {
                null
            }

        val created =
            requireNotNull(
                runtime.walletEntryCreateService.create(
                    userId = userId,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.TRANSFER,
                            groupId = groupId,
                            originId = originId,
                            targetId = targetId,
                            date = date,
                            originValue = value.toBigDecimalSafe(),
                            targetValue = targetValue?.toBigDecimalSafe(),
                            name = name,
                            confirmed = confirmed,
                            paymentType = paymentType,
                            installments = installments,
                            periodicity = periodicity,
                            periodicityQtyLimit = periodicityQtyLimit,
                            originBillDate = resolvedOriginBillDate,
                            targetBillDate = resolvedTargetBillDate,
                        ),
                ),
            ) { "Transfer was rejected due to insufficient permissions for selected group/origin/target" }

        context.lastWalletEventId = resolver.extractWalletEventId(created)
    }

    suspend fun payBill(
        amount: Number,
        date: LocalDate,
        bankAccountId: UUID? = null,
        billId: UUID? = null,
        billDate: LocalDate? = null,
        creditCardId: UUID? = null,
        observations: String? = null,
    ) {
        val userId = resolver.ensureUser()
        val bankAccount = resolver.resolveBankAccountItem(bankAccountId)
        val bill =
            resolver.resolveBill(
                billId = billId,
                billDate = billDate,
                creditCardId = creditCardId,
            )

        runtime.creditCardBillPaymentService.payBill(
            userId = userId,
            billId = requireNotNull(bill.id),
            bankAccountId = requireNotNull(bankAccount.id),
            date = date,
            amount = amount.toBigDecimalSafe(),
            observations = observations,
        )
    }

    suspend fun editOneOff(
        newEntryRequest: NewEntryRequest,
        walletEventId: UUID? = null,
    ) {
        val userId = resolver.ensureUser()
        val edited =
            runtime.walletEntryEditService.editOneOff(
                userId = userId,
                walletEventId = walletEventId ?: resolver.requireLastWalletEventId(),
                request = newEntryRequest,
            )

        context.lastWalletEventId = resolver.extractWalletEventId(edited)
    }

    suspend fun editScheduled(
        occurrenceDate: LocalDate,
        scope: ScheduledEditScope,
        newEntryRequest: NewEntryRequest,
        recurrenceConfigId: UUID? = null,
    ) {
        val userId = resolver.ensureUser()

        val edited =
            runtime.walletEntryEditService.editScheduled(
                userId = userId,
                recurrenceConfigId = recurrenceConfigId ?: resolver.requireRecurrenceEntity().id!!,
                request =
                    EditScheduledEntryRequest(
                        occurrenceDate = occurrenceDate,
                        scope = scope,
                        entry = newEntryRequest,
                    ),
            )

        context.lastRecurrenceConfigId = resolver.extractRecurrenceId(edited)
        context.lastWalletEventId = resolver.extractWalletEventId(edited)
    }

    suspend fun deleteOneOff(walletEventId: UUID? = null) {
        val userId = resolver.ensureUser()
        val deleted =
            runtime.walletEntryRemovalService.deleteOneOff(
                userId = userId,
                walletEventId = walletEventId ?: resolver.requireLastWalletEventId(),
            )

        context.lastRecurrenceConfigId = resolver.extractRecurrenceId(deleted)
        context.lastWalletEventId = resolver.extractWalletEventId(deleted)
    }

    suspend fun deleteScheduled(
        occurrenceDate: LocalDate,
        scope: ScheduledEditScope? = null,
        recurrenceConfigId: UUID? = null,
    ) {
        val userId = resolver.ensureUser()
        val deleted =
            runtime.walletEntryRemovalService.deleteScheduled(
                userId = userId,
                recurrenceConfigId = recurrenceConfigId ?: resolver.requireRecurrenceEntity().id!!,
                request =
                    DeleteScheduledEntryRequest(
                        occurrenceDate = occurrenceDate,
                        scope = scope,
                    ),
            )

        context.lastRecurrenceConfigId = resolver.extractRecurrenceId(deleted)
        context.lastWalletEventId = resolver.extractWalletEventId(deleted)
    }

    suspend fun clearPublishedEvents() {
        runtime.walletEventActionEventService.clear()
    }

    suspend fun fetchWalletEventById(walletEventId: UUID? = null) {
        context.lastFetchedWalletEvent = resolver.fetchWalletEventById(walletEventId)
    }

    suspend fun fetchScheduledByRecurrenceConfigId(recurrenceConfigId: UUID? = null) {
        context.lastFetchedScheduledWalletEvent = resolver.fetchScheduledEventByRecurrenceConfigId(recurrenceConfigId)
    }

    suspend fun fetchFirstScheduledExecution(filter: ScheduledExecutionFilter = ScheduledExecutionFilter.ALL) {
        context.lastFetchedScheduledWalletEvent = resolver.listScheduledExecutionEntries(filter).firstOrNull()
    }

    suspend fun fetchOverview(selectedMonth: YearMonth = YearMonth.from(runtime.clock.today())) {
        val userId = resolver.ensureUser()
        context.lastOverview =
            runtime.overviewDashboardService.getOverview(
                userId = userId,
                defaultCurrency = context.currentCurrency,
                selectedMonth = selectedMonth,
            )
    }

    fun lastWalletEventId(): UUID = resolver.requireLastWalletEventId()

    suspend fun runRecurrence() {
        runtime.recurrenceService.queueAllPendingOfExecution()

        while (true) {
            val queued = runtime.queueProducer.poll() ?: break
            val created =
                runtime.walletEntryCreateService.createFromRecurrenceConfig(
                    recurrenceConfigId = queued.entryRecurrenceConfigId,
                    date = queued.date,
                )
            context.lastRecurrenceConfigId = queued.entryRecurrenceConfigId
            context.lastWalletEventId = resolver.extractWalletEventId(created)
        }
    }

    suspend fun scheduledManagerCount(filter: com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter): Int =
        resolver.listScheduledExecutions(filter)

    fun advanceTime(to: LocalDate) {
        runtime.clock.setDate(to)
    }

    suspend fun advanceTimeToNextRecurrenceExecution(recurrenceConfigId: UUID? = null) {
        runtime.clock.setDate(resolver.getNextRecurrenceExecutionDate(recurrenceConfigId))
    }
}
