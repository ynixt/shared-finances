package com.ynixt.sharedfinances.resources.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.isSameMonthYear
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.withStartOfMonth
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.SimulatedOccurrence
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceOccurrenceSimulationService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class RecurrenceOccurrenceSimulationServiceImpl(
    private val creditCardBillService: CreditCardBillService,
) : RecurrenceOccurrenceSimulationService {
    private val maxSimulationIterationsPerConfig = 1024

    private data class ExpansionMode(
        val byDate: Boolean,
        val byBillDate: Boolean,
    )

    private data class BillWindow(
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    private data class SimulationState(
        val executionDate: LocalDate,
        val localInstallment: Int?,
        val globalInstallment: Int?,
        val billDateByWalletItemId: Map<UUID, LocalDate?>,
    )

    override suspend fun buildOccurrences(
        config: RecurrenceEventEntity,
        walletItems: List<WalletItem>,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        askedBillDate: LocalDate?,
        askedWalletItemId: UUID?,
        requestUserId: UUID?,
    ): List<SimulatedOccurrence> {
        val initialExecutionDate = config.nextExecution ?: return emptyList()
        val recurrenceEntries = config.entries!!.filterIsInstance<RecurrenceEntryEntity>()
        val mode =
            ExpansionMode(
                byDate = askedBillDate == null && maximumDate != null,
                byBillDate = askedBillDate != null && askedWalletItemId != null,
            )
        val localInstallment = if (config.paymentType == PaymentType.INSTALLMENTS) config.qtyExecuted + 1 else null
        val initialState =
            SimulationState(
                executionDate = initialExecutionDate,
                localInstallment = localInstallment,
                globalInstallment = localInstallment?.let { config.seriesOffset + it },
                billDateByWalletItemId = recurrenceEntries.associate { it.walletItemId to it.nextBillDate },
            )

        if (!mode.byDate && !mode.byBillDate) {
            return listOf(initialState.toOccurrence())
        }

        val askedWalletItem = askedWalletItemId?.let { askedId -> walletItems.find { it.id == askedId } }
        val askedBillWindow = resolveBillWindow(askedWalletItem, askedBillDate, requestUserId)

        return simulateOccurrences(
            config = config,
            initialState = initialState,
            mode = mode,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
            askedBillDate = askedBillDate,
            askedBillWindow = askedBillWindow,
            askedWalletItemId = askedWalletItemId,
        )
    }

    private fun simulateOccurrences(
        config: RecurrenceEventEntity,
        initialState: SimulationState,
        mode: ExpansionMode,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        askedBillDate: LocalDate?,
        askedBillWindow: BillWindow?,
        askedWalletItemId: UUID?,
    ): List<SimulatedOccurrence> {
        val occurrences = mutableListOf<SimulatedOccurrence>()
        var state = initialState

        repeat(maxSimulationIterationsPerConfig) {
            if (shouldStopBeforeCollect(state, mode, askedBillWindow, askedBillDate, askedWalletItemId)) {
                return occurrences
            }

            if (shouldCollect(state, mode, minimumDate, maximumDate, askedBillDate, askedBillWindow, askedWalletItemId)) {
                occurrences.add(state.toOccurrence())
            }

            if (shouldStopAfterCollect(state, mode, maximumDate, config)) {
                return occurrences
            }

            state = advanceState(state, config.periodicity)
        }

        return occurrences
    }

    private fun shouldStopBeforeCollect(
        state: SimulationState,
        mode: ExpansionMode,
        askedBillWindow: BillWindow?,
        askedBillDate: LocalDate?,
        askedWalletItemId: UUID?,
    ): Boolean {
        if (!mode.byBillDate) return false

        if (askedBillWindow != null) {
            return state.executionDate.isAfter(askedBillWindow.endDate)
        }

        val selectedBillDate = askedWalletItemId?.let { walletItemId -> state.billDateByWalletItemId[walletItemId] }
        return selectedBillDate == null || selectedBillDate.isAfter(askedBillDate)
    }

    private fun shouldCollect(
        state: SimulationState,
        mode: ExpansionMode,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        askedBillDate: LocalDate?,
        askedBillWindow: BillWindow?,
        askedWalletItemId: UUID?,
    ): Boolean {
        if (mode.byDate) {
            val lowerDate = minimumDate ?: state.executionDate
            return !state.executionDate.isBefore(lowerDate) && !state.executionDate.isAfter(maximumDate)
        }

        if (!mode.byBillDate) {
            return false
        }

        if (askedBillWindow != null) {
            return !state.executionDate.isBefore(askedBillWindow.startDate) &&
                !state.executionDate.isAfter(askedBillWindow.endDate)
        }

        val selectedBillDate = askedWalletItemId?.let { walletItemId -> state.billDateByWalletItemId[walletItemId] }
        return selectedBillDate == askedBillDate
    }

    private fun shouldStopAfterCollect(
        state: SimulationState,
        mode: ExpansionMode,
        maximumDate: LocalDate?,
        config: RecurrenceEventEntity,
    ): Boolean {
        if (config.endExecution != null && !state.executionDate.isBefore(config.endExecution)) {
            return true
        }

        if (config.qtyLimit != null && state.localInstallment != null && state.localInstallment >= config.qtyLimit) {
            return true
        }

        val nextExecutionDate = calculateNextDate(state.executionDate, config.periodicity)
        if (!nextExecutionDate.isAfter(state.executionDate)) {
            return true
        }

        if (mode.byDate && nextExecutionDate.isAfter(maximumDate)) {
            return true
        }

        return false
    }

    private fun advanceState(
        state: SimulationState,
        periodicity: RecurrenceType,
    ): SimulationState =
        SimulationState(
            executionDate = calculateNextDate(state.executionDate, periodicity),
            localInstallment = state.localInstallment?.plus(1),
            globalInstallment = state.globalInstallment?.plus(1),
            billDateByWalletItemId =
                state.billDateByWalletItemId.mapValues { (_, billDate) ->
                    if (billDate == null) null else calculateNextBillDate(billDate, periodicity)
                },
        )

    private fun calculateNextDate(
        date: LocalDate,
        periodicity: RecurrenceType,
    ): LocalDate =
        when (periodicity) {
            RecurrenceType.SINGLE -> date
            RecurrenceType.DAILY -> date.plusDays(1)
            RecurrenceType.WEEKLY -> date.plusWeeks(1)
            RecurrenceType.MONTHLY -> date.plusMonths(1)
            RecurrenceType.YEARLY -> date.plusYears(1)
        }

    private fun calculateNextBillDate(
        billDate: LocalDate,
        periodicity: RecurrenceType,
    ): LocalDate {
        val candidate = calculateNextDate(billDate, periodicity)
        return if (candidate.isSameMonthYear(billDate)) billDate else candidate.withDayOfMonth(1)
    }

    private suspend fun resolveBillWindow(
        walletItem: WalletItem?,
        billDate: LocalDate?,
        userId: UUID?,
    ): BillWindow? {
        if (walletItem !is CreditCard || billDate == null || userId == null) return null

        val fixedBillDate = billDate.withStartOfMonth()
        val currentBill =
            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = walletItem.id!!,
                billDate = fixedBillDate,
            )
        val previousBill =
            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = walletItem.id!!,
                billDate = fixedBillDate.minusMonths(1),
            )

        return BillWindow(
            startDate = previousBill.closingDate.plusDays(1),
            endDate = currentBill.closingDate,
        )
    }

    private fun SimulationState.toOccurrence(): SimulatedOccurrence =
        SimulatedOccurrence(
            executionDate = executionDate,
            installment = globalInstallment,
            billDateByWalletItemId = billDateByWalletItemId,
        )
}
