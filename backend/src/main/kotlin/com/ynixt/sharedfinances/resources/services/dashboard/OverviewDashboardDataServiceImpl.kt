package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashDirection
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseSourceSummary
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.collections.mapNotNull

@Service
internal class OverviewDashboardDataServiceImpl(
    private val walletItemRepository: WalletItemRepository,
    private val walletItemMapper: WalletItemMapper,
    private val walletEntryRepository: WalletEntryRepository,
    private val recurrenceSimulationService: RecurrenceSimulationService,
    private val creditCardBillService: CreditCardBillService,
    private val clock: Clock,
) {
    internal suspend fun loadVisibleItems(userId: UUID): OverviewDashboardVisibleItems {
        val visibleWalletItems: List<WalletItem> =
            walletItemRepository
                .findAllByUserIdAndEnabledAndShowOnDashboard(
                    userId = userId,
                    enabled = true,
                    showOnDashboard = true,
                    pageable = Pageable.unpaged(),
                ).map(walletItemMapper::toModel)
                .collectList()
                .awaitSingle()

        val visibleBankAccounts = visibleWalletItems.filterIsInstance<BankAccount>()
        val visibleCreditCards = visibleWalletItems.filterIsInstance<CreditCard>()
        val bankAccountById = visibleBankAccounts.associateBy { it.id!! }

        return OverviewDashboardVisibleItems(
            items = visibleWalletItems,
            bankAccounts = visibleBankAccounts,
            creditCards = visibleCreditCards,
            walletItemIds = visibleWalletItems.mapNotNull { it.id }.toSet(),
            bankAccountById = bankAccountById,
            bankAccountIds = bankAccountById.keys,
        )
    }

    internal suspend fun fetchExecutedByMonthByBank(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Map<YearMonth, Map<UUID, MonthlyAmount>> {
        if (minimumDate.isAfter(maximumDate)) {
            return emptyMap()
        }

        val map = mutableMapOf<YearMonth, MutableMap<UUID, MonthlyAmount>>()

        walletEntryRepository
            .summarizeBankAccountsByMonth(
                userId = userId,
                minimumDate = minimumDate,
                maximumDate = maximumDate,
            ).collectList()
            .awaitSingle()
            .forEach { summary ->
                val byWallet = map.getOrPut(summary.month) { mutableMapOf() }
                val current = byWallet.getOrDefault(summary.walletItemId, MonthlyAmount.ZERO)
                byWallet[summary.walletItemId] =
                    current +
                    MonthlyAmount(
                        net = summary.net,
                        cashIn = summary.cashIn,
                        cashOut = summary.cashOut,
                    )
            }

        return map
    }

    internal suspend fun loadExecutedOverviewContext(
        userId: UUID,
        chartStartMonth: YearMonth,
        selectedMonth: YearMonth,
        maximumExecutedDate: LocalDate,
    ): ExecutedOverviewContext {
        val chartMinimumDate = chartStartMonth.atDay(1)
        if (chartMinimumDate.isAfter(maximumExecutedDate)) {
            return ExecutedOverviewContext.EMPTY
        }

        val bankFacts =
            walletEntryRepository
                .summarizeOverviewBankFacts(
                    userId = userId,
                    minimumDate = chartMinimumDate,
                    maximumDate = maximumExecutedDate,
                ).collectList()
                .awaitSingle()

        val expenseFacts =
            walletEntryRepository
                .summarizeOverviewExpenseFacts(
                    userId = userId,
                    minimumDate = chartMinimumDate,
                    maximumDate = maximumExecutedDate,
                ).collectList()
                .awaitSingle()

        return ExecutedOverviewContext(
            executedByMonthByBankId = bankFacts.toExecutedByMonthByBankId(),
            expenseChartContributions = expenseFacts.toExpenseChartContributions(),
            expenseSourceSummaries = expenseFacts.toExpenseSourceSummaries(selectedMonth),
            cashBreakdownContributions =
                buildExecutedCashBreakdownContributions(
                    selectedMonth = selectedMonth,
                    bankFacts = bankFacts,
                ),
            expenseBreakdownContributions =
                buildExecutedExpenseBreakdownContributions(
                    selectedMonth = selectedMonth,
                    expenseFacts = expenseFacts,
                ),
        )
    }

    internal suspend fun fetchExecutedExpenseChartContributions(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): List<RawChartContribution> =
        if (minimumDate.isAfter(maximumDate)) {
            emptyList()
        } else {
            walletEntryRepository
                .summarizeOverviewExpenseByMonth(
                    userId = userId,
                    minimumDate = minimumDate,
                    maximumDate = maximumDate,
                ).collectList()
                .awaitSingle()
                .map { summary ->
                    RawChartContribution(
                        chartSeries = ChartSeries.EXPENSE,
                        component = ChartPointComponent.EXECUTED,
                        month = summary.month,
                        value = summary.expense,
                        currency = summary.currency,
                        referenceDate = summary.month.atEndOfMonth(),
                    )
                }
        }

    internal suspend fun fetchExecutedExpenseBySource(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): List<OverviewExpenseSourceSummary> =
        if (minimumDate.isAfter(maximumDate)) {
            emptyList()
        } else {
            walletEntryRepository
                .summarizeOverviewExpenseBySource(
                    userId = userId,
                    minimumDate = minimumDate,
                    maximumDate = maximumDate,
                ).collectList()
                .awaitSingle()
        }

    internal suspend fun fetchExecutedCashBreakdownContributions(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
        referenceDate: LocalDate,
    ): List<RawBreakdownContribution> =
        walletEntryRepository
            .summarizeOverviewCashBreakdown(
                userId = userId,
                minimumDate = minimumDate,
                maximumDate = maximumDate,
            ).collectList()
            .awaitSingle()
            .map { summary ->
                RawBreakdownContribution(
                    breakdownType =
                        when (summary.direction) {
                            OverviewCashDirection.IN -> BreakdownType.CASH_IN_CATEGORY
                            OverviewCashDirection.OUT -> BreakdownType.CASH_OUT_CATEGORY
                        },
                    component = ChartPointComponent.EXECUTED,
                    sliceId = summary.categoryId,
                    label = summary.categoryName ?: PREDEFINED_UNCATEGORIZED_LABEL,
                    value = summary.amount,
                    currency = summary.currency,
                    referenceDate = referenceDate,
                )
            }

    internal suspend fun fetchExecutedExpenseBreakdownContributions(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
        referenceDate: LocalDate,
    ): List<RawBreakdownContribution> =
        walletEntryRepository
            .summarizeOverviewExpenseBreakdown(
                userId = userId,
                minimumDate = minimumDate,
                maximumDate = maximumDate,
            ).collectList()
            .awaitSingle()
            .flatMap { summary ->
                listOf(
                    RawBreakdownContribution(
                        breakdownType = BreakdownType.EXPENSE_GROUP,
                        component = ChartPointComponent.EXECUTED,
                        sliceId = summary.groupId,
                        label = summary.groupName ?: PREDEFINED_INDIVIDUAL_LABEL,
                        value = summary.expense,
                        currency = summary.currency,
                        referenceDate = referenceDate,
                    ),
                    RawBreakdownContribution(
                        breakdownType = BreakdownType.EXPENSE_CATEGORY,
                        component = ChartPointComponent.EXECUTED,
                        sliceId = summary.categoryId,
                        label = summary.categoryName ?: PREDEFINED_UNCATEGORIZED_LABEL,
                        value = summary.expense,
                        currency = summary.currency,
                        referenceDate = referenceDate,
                    ),
                )
            }

    internal suspend fun loadProjectedOverviewContext(
        userId: UUID,
        today: LocalDate,
        currentMonth: YearMonth,
        selectedMonth: YearMonth,
        visibleItems: OverviewDashboardVisibleItems,
    ): ProjectedOverviewContext {
        if (selectedMonth.isBefore(currentMonth) || visibleItems.walletItemIds.isEmpty()) {
            return ProjectedOverviewContext.EMPTY
        }

        val selectedMonthEnd = selectedMonth.atEndOfMonth()
        val minimumProjectedDate = today
        val simulatedEvents =
            if (selectedMonthEnd.isBefore(minimumProjectedDate)) {
                emptyList()
            } else {
                recurrenceSimulationService.simulateGenerationWithFilters(
                    minimumEndExecution = minimumProjectedDate,
                    maximumNextExecution = selectedMonthEnd,
                    userId = userId,
                    walletItemId = null,
                    billDate = null,
                    groupIds = emptySet(),
                    walletItemIds = visibleItems.walletItemIds,
                    entryTypes = emptySet(),
                )
            }

        return ProjectedOverviewContext(
            projectedByMonthByBankId =
                buildProjectedByMonthByBankId(
                    userId = userId,
                    visibleBankAccountIds = visibleItems.bankAccountIds,
                    simulatedEvents = simulatedEvents,
                ),
            projectedCreditCardDetailsByMonth =
                buildProjectedCreditCardCashOutByMonth(
                    userId = userId,
                    currentMonth = currentMonth,
                    selectedMonth = selectedMonth,
                    visibleCreditCards = visibleItems.creditCards,
                    simulatedEvents = simulatedEvents,
                ),
            projectedExpenseContributions =
                buildProjectedExpenseContributions(
                    selectedMonth = selectedMonth,
                    visibleWalletItemIds = visibleItems.walletItemIds,
                    simulatedEvents = simulatedEvents,
                ),
            projectedCashBreakdownContributions =
                buildProjectedCashBreakdownContributions(
                    userId = userId,
                    selectedMonth = selectedMonth,
                    visibleBankAccountIds = visibleItems.bankAccountIds,
                    simulatedEvents = simulatedEvents,
                ),
        )
    }

    internal suspend fun fetchProjectedByMonthByBank(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
        visibleBankAccountIds: Set<UUID>,
    ): Map<YearMonth, Map<UUID, MonthlyAmount>> {
        if (minimumDate.isAfter(maximumDate) || visibleBankAccountIds.isEmpty()) {
            return emptyMap()
        }

        val map = mutableMapOf<YearMonth, MutableMap<UUID, MonthlyAmount>>()

        recurrenceSimulationService
            .simulateGeneration(
                minimumEndExecution = minimumDate,
                maximumNextExecution = maximumDate,
                userId = userId,
                groupIds = emptySet(),
                walletItemId = null,
                billDate = null,
            ).forEach { simulated ->
                val month = YearMonth.from(simulated.date)
                val byWallet = map.getOrPut(month) { mutableMapOf() }
                val isInternalBankTransfer = isInternalBankTransfer(userId, simulated)

                simulated.entries.forEach { entry ->
                    if (!visibleBankAccountIds.contains(entry.walletItemId)) {
                        return@forEach
                    }

                    val value = entry.value
                    val current = byWallet.getOrDefault(entry.walletItemId, MonthlyAmount.ZERO)
                    byWallet[entry.walletItemId] =
                        current +
                        MonthlyAmount(
                            net = value,
                            cashIn = if (value > BigDecimal.ZERO && !isInternalBankTransfer) value else BigDecimal.ZERO,
                            cashOut = if (value < BigDecimal.ZERO && !isInternalBankTransfer) value.abs() else BigDecimal.ZERO,
                        )
                }
            }

        return map
    }

    internal suspend fun fetchProjectedCashBreakdownContributions(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
        selectedMonth: YearMonth,
        visibleBankAccountIds: Set<UUID>,
    ): List<RawBreakdownContribution> {
        if (minimumDate.isAfter(maximumDate) || visibleBankAccountIds.isEmpty()) {
            return emptyList()
        }

        val selectedMonthEnd = selectedMonth.atEndOfMonth()
        val projectedContributions = mutableListOf<RawBreakdownContribution>()

        recurrenceSimulationService
            .simulateGenerationWithFilters(
                minimumEndExecution = minimumDate,
                maximumNextExecution = maximumDate,
                userId = userId,
                groupIds = emptySet(),
                walletItemId = null,
                billDate = null,
                walletItemIds = visibleBankAccountIds,
                entryTypes = emptySet(),
            ).forEach { simulated ->
                if (YearMonth.from(simulated.date) != selectedMonth) {
                    return@forEach
                }

                val isInternalBankTransfer = isInternalBankTransfer(userId, simulated)

                simulated.entries.forEach { entry ->
                    if (
                        !visibleBankAccountIds.contains(entry.walletItemId) ||
                        entry.walletItem.type != WalletItemType.BANK_ACCOUNT ||
                        entry.value.compareTo(BigDecimal.ZERO) == 0 ||
                        isInternalBankTransfer
                    ) {
                        return@forEach
                    }

                    projectedContributions.add(
                        RawBreakdownContribution(
                            breakdownType =
                                if (entry.value > BigDecimal.ZERO) {
                                    BreakdownType.CASH_IN_CATEGORY
                                } else {
                                    BreakdownType.CASH_OUT_CATEGORY
                                },
                            component = ChartPointComponent.PROJECTED,
                            sliceId = simulated.category?.id,
                            label = simulated.category?.name ?: PREDEFINED_UNCATEGORIZED_LABEL,
                            value = entry.value.abs(),
                            currency = entry.walletItem.currency,
                            referenceDate = selectedMonthEnd,
                        ),
                    )
                }
            }

        return projectedContributions
    }

    internal suspend fun fetchProjectedExpenseContributions(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
        selectedMonth: YearMonth,
        visibleWalletItemIds: Set<UUID>,
    ): ProjectedExpenseContributions {
        if (minimumDate.isAfter(maximumDate) || visibleWalletItemIds.isEmpty()) {
            return ProjectedExpenseContributions.EMPTY
        }

        val projectedChartContributions = mutableListOf<RawChartContribution>()
        val projectedBreakdownContributions = mutableListOf<RawBreakdownContribution>()
        val projectedDetailsByWalletId = mutableMapOf<UUID, OverviewExpenseSourceSummary>()
        val selectedMonthEnd = selectedMonth.atEndOfMonth()

        recurrenceSimulationService
            .simulateGenerationWithFilters(
                minimumEndExecution = minimumDate,
                maximumNextExecution = maximumDate,
                userId = userId,
                walletItemId = null,
                billDate = null,
                groupIds = emptySet(),
                walletItemIds = visibleWalletItemIds,
                entryTypes = setOf(WalletEntryType.EXPENSE),
            ).forEach { simulated ->
                if (simulated.type != WalletEntryType.EXPENSE) {
                    return@forEach
                }

                val month = YearMonth.from(simulated.date)
                val includeBreakdownForSelectedMonth = month == selectedMonth

                simulated.entries.forEach { entry ->
                    if (!visibleWalletItemIds.contains(entry.walletItemId) || entry.value >= BigDecimal.ZERO) {
                        return@forEach
                    }

                    val projectedExpense = entry.value.abs()
                    projectedChartContributions.add(
                        RawChartContribution(
                            chartSeries = ChartSeries.EXPENSE,
                            component = ChartPointComponent.PROJECTED,
                            month = month,
                            value = projectedExpense,
                            currency = entry.walletItem.currency,
                            referenceDate = month.atEndOfMonth(),
                        ),
                    )

                    if (includeBreakdownForSelectedMonth) {
                        projectedBreakdownContributions.add(
                            RawBreakdownContribution(
                                breakdownType = BreakdownType.EXPENSE_GROUP,
                                component = ChartPointComponent.PROJECTED,
                                sliceId = simulated.group?.id,
                                label = simulated.group?.name ?: PREDEFINED_INDIVIDUAL_LABEL,
                                value = projectedExpense,
                                currency = entry.walletItem.currency,
                                referenceDate = selectedMonthEnd,
                            ),
                        )
                        projectedBreakdownContributions.add(
                            RawBreakdownContribution(
                                breakdownType = BreakdownType.EXPENSE_CATEGORY,
                                component = ChartPointComponent.PROJECTED,
                                sliceId = simulated.category?.id,
                                label = simulated.category?.name ?: PREDEFINED_UNCATEGORIZED_LABEL,
                                value = projectedExpense,
                                currency = entry.walletItem.currency,
                                referenceDate = selectedMonthEnd,
                            ),
                        )

                        val current = projectedDetailsByWalletId[entry.walletItemId]
                        projectedDetailsByWalletId[entry.walletItemId] =
                            if (current == null) {
                                OverviewExpenseSourceSummary(
                                    walletItemId = entry.walletItemId,
                                    walletItemName = entry.walletItem.name,
                                    walletItemType = entry.walletItem.type,
                                    currency = entry.walletItem.currency,
                                    expense = projectedExpense,
                                )
                            } else {
                                current.copy(expense = current.expense.add(projectedExpense))
                            }
                    }
                }
            }

        return ProjectedExpenseContributions(
            chartContributions = projectedChartContributions,
            breakdownContributions = projectedBreakdownContributions,
            selectedMonthDetails = projectedDetailsByWalletId.values.sortedBy { it.walletItemName.lowercase() },
        )
    }

    internal suspend fun fetchProjectedCreditCardCashOutByMonth(
        userId: UUID,
        minimumMonth: YearMonth,
        maximumMonth: YearMonth,
        creditCards: List<CreditCard>,
    ): Map<YearMonth, List<ProjectedCreditCardExpense>> {
        if (creditCards.isEmpty() || maximumMonth.isBefore(minimumMonth)) {
            return emptyMap()
        }

        val visibleCardsById = creditCards.mapNotNull { card -> card.id?.let { it to card } }.toMap()
        if (visibleCardsById.isEmpty()) {
            return emptyMap()
        }

        val minimumDueDate = minimumMonth.atDay(1)
        val maximumDueDate = maximumMonth.atEndOfMonth()
        val projectedByMonthAndCardId = mutableMapOf<Pair<YearMonth, UUID>, BigDecimal>()

        creditCardBillService
            .findAllOpenByDueDateBetween(
                userId = userId,
                minimumDueDate = minimumDueDate,
                maximumDueDate = maximumDueDate,
            ).forEach { bill ->
                val remainingUnpaid = bill.value.negate().max(BigDecimal.ZERO)
                if (remainingUnpaid.compareTo(BigDecimal.ZERO) <= 0) {
                    return@forEach
                }

                val key = YearMonth.from(bill.dueDate) to bill.creditCardId
                projectedByMonthAndCardId[key] =
                    projectedByMonthAndCardId
                        .getOrDefault(key, BigDecimal.ZERO)
                        .add(remainingUnpaid)
            }

        val today = LocalDate.now(clock)
        val minimumProjectedDate = maxOf(today.plusDays(1), minimumDueDate)
        if (!maximumDueDate.isBefore(minimumProjectedDate)) {
            recurrenceSimulationService
                .simulateGeneration(
                    minimumEndExecution = minimumProjectedDate,
                    maximumNextExecution = maximumDueDate,
                    userId = userId,
                    groupIds = emptySet(),
                    walletItemId = null,
                    billDate = null,
                ).flatMap { event ->
                    event.entries.asSequence()
                }.filter { entry ->
                    entry.walletItem.type == WalletItemType.CREDIT_CARD &&
                        visibleCardsById.containsKey(entry.walletItemId) &&
                        entry.billDate != null &&
                        !YearMonth.from(entry.billDate).isBefore(minimumMonth) &&
                        !YearMonth.from(entry.billDate).isAfter(maximumMonth)
                }.groupBy({ entry -> YearMonth.from(entry.billDate!!) to entry.walletItemId }, { entry -> entry.value })
                .forEach { (key, values) ->
                    val projectedAmount = values.fold(BigDecimal.ZERO, BigDecimal::add).negate().max(BigDecimal.ZERO)
                    if (projectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        return@forEach
                    }

                    projectedByMonthAndCardId[key] =
                        projectedByMonthAndCardId
                            .getOrDefault(key, BigDecimal.ZERO)
                            .add(projectedAmount)
                }
        }

        return projectedByMonthAndCardId.entries
            .groupBy({ it.key.first }, { it.key.second to it.value })
            .mapValues { (_, expensesByCard) ->
                expensesByCard
                    .mapNotNull { (creditCardId, projectedExpense) ->
                        val creditCard = visibleCardsById[creditCardId] ?: return@mapNotNull null
                        if (projectedExpense.compareTo(BigDecimal.ZERO) <= 0) {
                            return@mapNotNull null
                        }

                        ProjectedCreditCardExpense(
                            creditCardId = creditCardId,
                            creditCardName = creditCard.name,
                            currency = creditCard.currency,
                            projectedExpense = projectedExpense,
                        )
                    }.sortedBy { it.creditCardName.lowercase() }
            }
    }

    private fun isInternalBankTransfer(
        userId: UUID,
        simulated: com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse,
    ): Boolean =
        simulated.type == WalletEntryType.TRANSFER &&
            simulated.entries.size == 2 &&
            simulated.entries.all { entry ->
                entry.walletItem.type == WalletItemType.BANK_ACCOUNT &&
                    entry.walletItem.userId == userId
            }

    private fun buildProjectedByMonthByBankId(
        userId: UUID,
        visibleBankAccountIds: Set<UUID>,
        simulatedEvents: List<com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse>,
    ): Map<YearMonth, Map<UUID, MonthlyAmount>> {
        if (visibleBankAccountIds.isEmpty() || simulatedEvents.isEmpty()) {
            return emptyMap()
        }

        val map = mutableMapOf<YearMonth, MutableMap<UUID, MonthlyAmount>>()

        simulatedEvents.forEach { simulated ->
            val month = YearMonth.from(simulated.date)
            val byWallet = map.getOrPut(month) { mutableMapOf() }
            val isInternalBankTransfer = isInternalBankTransfer(userId, simulated)

            simulated.entries.forEach { entry ->
                if (!visibleBankAccountIds.contains(entry.walletItemId)) {
                    return@forEach
                }

                val value = entry.value
                val current = byWallet.getOrDefault(entry.walletItemId, MonthlyAmount.ZERO)
                byWallet[entry.walletItemId] =
                    current +
                    MonthlyAmount(
                        net = value,
                        cashIn = if (value > BigDecimal.ZERO && !isInternalBankTransfer) value else BigDecimal.ZERO,
                        cashOut = if (value < BigDecimal.ZERO && !isInternalBankTransfer) value.abs() else BigDecimal.ZERO,
                    )
            }
        }

        return map
    }

    private fun buildProjectedCashBreakdownContributions(
        userId: UUID,
        selectedMonth: YearMonth,
        visibleBankAccountIds: Set<UUID>,
        simulatedEvents: List<com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse>,
    ): List<RawBreakdownContribution> {
        if (visibleBankAccountIds.isEmpty() || simulatedEvents.isEmpty()) {
            return emptyList()
        }

        val selectedMonthEnd = selectedMonth.atEndOfMonth()
        val projectedContributions = mutableListOf<RawBreakdownContribution>()

        simulatedEvents.forEach { simulated ->
            if (YearMonth.from(simulated.date) != selectedMonth) {
                return@forEach
            }

            val isInternalBankTransfer = isInternalBankTransfer(userId, simulated)

            simulated.entries.forEach { entry ->
                if (
                    !visibleBankAccountIds.contains(entry.walletItemId) ||
                    entry.walletItem.type != WalletItemType.BANK_ACCOUNT ||
                    entry.value.compareTo(BigDecimal.ZERO) == 0 ||
                    isInternalBankTransfer
                ) {
                    return@forEach
                }

                projectedContributions.add(
                    RawBreakdownContribution(
                        breakdownType =
                            if (entry.value > BigDecimal.ZERO) {
                                BreakdownType.CASH_IN_CATEGORY
                            } else {
                                BreakdownType.CASH_OUT_CATEGORY
                            },
                        component = ChartPointComponent.PROJECTED,
                        sliceId = simulated.category?.id,
                        label = simulated.category?.name ?: PREDEFINED_UNCATEGORIZED_LABEL,
                        value = entry.value.abs(),
                        currency = entry.walletItem.currency,
                        referenceDate = selectedMonthEnd,
                    ),
                )
            }
        }

        return projectedContributions
    }

    private fun buildProjectedExpenseContributions(
        selectedMonth: YearMonth,
        visibleWalletItemIds: Set<UUID>,
        simulatedEvents: List<com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse>,
    ): ProjectedExpenseContributions {
        if (visibleWalletItemIds.isEmpty() || simulatedEvents.isEmpty()) {
            return ProjectedExpenseContributions.EMPTY
        }

        val projectedChartContributions = mutableListOf<RawChartContribution>()
        val projectedBreakdownContributions = mutableListOf<RawBreakdownContribution>()
        val projectedDetailsByWalletId = mutableMapOf<UUID, OverviewExpenseSourceSummary>()
        val selectedMonthEnd = selectedMonth.atEndOfMonth()

        simulatedEvents.forEach { simulated ->
            if (simulated.type != WalletEntryType.EXPENSE) {
                return@forEach
            }

            val month = YearMonth.from(simulated.date)
            val includeBreakdownForSelectedMonth = month == selectedMonth

            simulated.entries.forEach { entry ->
                if (!visibleWalletItemIds.contains(entry.walletItemId) || entry.value >= BigDecimal.ZERO) {
                    return@forEach
                }

                val projectedExpense = entry.value.abs()
                projectedChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.EXPENSE,
                        component = ChartPointComponent.PROJECTED,
                        month = month,
                        value = projectedExpense,
                        currency = entry.walletItem.currency,
                        referenceDate = month.atEndOfMonth(),
                    ),
                )

                if (includeBreakdownForSelectedMonth) {
                    projectedBreakdownContributions.add(
                        RawBreakdownContribution(
                            breakdownType = BreakdownType.EXPENSE_GROUP,
                            component = ChartPointComponent.PROJECTED,
                            sliceId = simulated.group?.id,
                            label = simulated.group?.name ?: PREDEFINED_INDIVIDUAL_LABEL,
                            value = projectedExpense,
                            currency = entry.walletItem.currency,
                            referenceDate = selectedMonthEnd,
                        ),
                    )
                    projectedBreakdownContributions.add(
                        RawBreakdownContribution(
                            breakdownType = BreakdownType.EXPENSE_CATEGORY,
                            component = ChartPointComponent.PROJECTED,
                            sliceId = simulated.category?.id,
                            label = simulated.category?.name ?: PREDEFINED_UNCATEGORIZED_LABEL,
                            value = projectedExpense,
                            currency = entry.walletItem.currency,
                            referenceDate = selectedMonthEnd,
                        ),
                    )

                    val current = projectedDetailsByWalletId[entry.walletItemId]
                    projectedDetailsByWalletId[entry.walletItemId] =
                        if (current == null) {
                            OverviewExpenseSourceSummary(
                                walletItemId = entry.walletItemId,
                                walletItemName = entry.walletItem.name,
                                walletItemType = entry.walletItem.type,
                                currency = entry.walletItem.currency,
                                expense = projectedExpense,
                            )
                        } else {
                            current.copy(expense = current.expense.add(projectedExpense))
                        }
                }
            }
        }

        return ProjectedExpenseContributions(
            chartContributions = projectedChartContributions,
            breakdownContributions = projectedBreakdownContributions,
            selectedMonthDetails = projectedDetailsByWalletId.values.sortedBy { it.walletItemName.lowercase() },
        )
    }

    private suspend fun buildProjectedCreditCardCashOutByMonth(
        userId: UUID,
        currentMonth: YearMonth,
        selectedMonth: YearMonth,
        visibleCreditCards: List<CreditCard>,
        simulatedEvents: List<com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse>,
    ): Map<YearMonth, List<ProjectedCreditCardExpense>> {
        if (visibleCreditCards.isEmpty() || selectedMonth.isBefore(currentMonth)) {
            return emptyMap()
        }

        val visibleCardsById = visibleCreditCards.mapNotNull { card -> card.id?.let { it to card } }.toMap()
        if (visibleCardsById.isEmpty()) {
            return emptyMap()
        }

        val minimumDueDate = currentMonth.atDay(1)
        val maximumDueDate = selectedMonth.atEndOfMonth()
        val projectedByMonthAndCardId = mutableMapOf<Pair<YearMonth, UUID>, BigDecimal>()

        creditCardBillService
            .findAllOpenByDueDateBetween(
                userId = userId,
                minimumDueDate = minimumDueDate,
                maximumDueDate = maximumDueDate,
            ).forEach { bill ->
                val remainingUnpaid = bill.value.negate().max(BigDecimal.ZERO)
                if (remainingUnpaid.compareTo(BigDecimal.ZERO) <= 0) {
                    return@forEach
                }

                val key = YearMonth.from(bill.dueDate) to bill.creditCardId
                projectedByMonthAndCardId[key] =
                    projectedByMonthAndCardId
                        .getOrDefault(key, BigDecimal.ZERO)
                        .add(remainingUnpaid)
            }

        simulatedEvents
            .asSequence()
            .flatMap { event -> event.entries.asSequence() }
            .filter { entry ->
                entry.walletItem.type == WalletItemType.CREDIT_CARD &&
                    visibleCardsById.containsKey(entry.walletItemId) &&
                    entry.billDate != null &&
                    !YearMonth.from(entry.billDate).isBefore(currentMonth) &&
                    !YearMonth.from(entry.billDate).isAfter(selectedMonth)
            }.groupBy({ entry -> YearMonth.from(entry.billDate!!) to entry.walletItemId }, { entry -> entry.value })
            .forEach { (key, values) ->
                val projectedAmount = values.fold(BigDecimal.ZERO, BigDecimal::add).negate().max(BigDecimal.ZERO)
                if (projectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    return@forEach
                }

                projectedByMonthAndCardId[key] =
                    projectedByMonthAndCardId
                        .getOrDefault(key, BigDecimal.ZERO)
                        .add(projectedAmount)
            }

        return projectedByMonthAndCardId.entries
            .groupBy({ it.key.first }, { it.key.second to it.value })
            .mapValues { (_, expensesByCard) ->
                expensesByCard
                    .mapNotNull { (creditCardId, projectedExpense) ->
                        val creditCard = visibleCardsById[creditCardId] ?: return@mapNotNull null
                        if (projectedExpense.compareTo(BigDecimal.ZERO) <= 0) {
                            return@mapNotNull null
                        }

                        ProjectedCreditCardExpense(
                            creditCardId = creditCardId,
                            creditCardName = creditCard.name,
                            currency = creditCard.currency,
                            projectedExpense = projectedExpense,
                        )
                    }.sortedBy { it.creditCardName.lowercase() }
            }
    }

    private fun buildExecutedCashBreakdownContributions(
        selectedMonth: YearMonth,
        bankFacts: List<com.ynixt.sharedfinances.domain.models.dashboard.OverviewExecutedBankFactSummary>,
    ): List<RawBreakdownContribution> {
        val selectedMonthEnd = selectedMonth.atEndOfMonth()

        return bankFacts
            .asSequence()
            .filter { it.month == selectedMonth }
            .flatMap { fact ->
                listOfNotNull(
                    fact.cashIn
                        .takeIf { it > BigDecimal.ZERO }
                        ?.let { amount ->
                            RawBreakdownContribution(
                                breakdownType = BreakdownType.CASH_IN_CATEGORY,
                                component = ChartPointComponent.EXECUTED,
                                sliceId = fact.categoryId,
                                label = fact.categoryName ?: PREDEFINED_UNCATEGORIZED_LABEL,
                                value = amount,
                                currency = fact.currency,
                                referenceDate = selectedMonthEnd,
                            )
                        },
                    fact.cashOut
                        .takeIf { it > BigDecimal.ZERO }
                        ?.let { amount ->
                            RawBreakdownContribution(
                                breakdownType = BreakdownType.CASH_OUT_CATEGORY,
                                component = ChartPointComponent.EXECUTED,
                                sliceId = fact.categoryId,
                                label = fact.categoryName ?: PREDEFINED_UNCATEGORIZED_LABEL,
                                value = amount,
                                currency = fact.currency,
                                referenceDate = selectedMonthEnd,
                            )
                        },
                ).asSequence()
            }.toList()
    }

    private fun buildExecutedExpenseBreakdownContributions(
        selectedMonth: YearMonth,
        expenseFacts: List<com.ynixt.sharedfinances.domain.models.dashboard.OverviewExecutedExpenseFactSummary>,
    ): List<RawBreakdownContribution> {
        val selectedMonthEnd = selectedMonth.atEndOfMonth()

        return expenseFacts
            .asSequence()
            .filter { it.month == selectedMonth }
            .flatMap { fact ->
                sequenceOf(
                    RawBreakdownContribution(
                        breakdownType = BreakdownType.EXPENSE_GROUP,
                        component = ChartPointComponent.EXECUTED,
                        sliceId = fact.groupId,
                        label = fact.groupName ?: PREDEFINED_INDIVIDUAL_LABEL,
                        value = fact.expense,
                        currency = fact.currency,
                        referenceDate = selectedMonthEnd,
                    ),
                    RawBreakdownContribution(
                        breakdownType = BreakdownType.EXPENSE_CATEGORY,
                        component = ChartPointComponent.EXECUTED,
                        sliceId = fact.categoryId,
                        label = fact.categoryName ?: PREDEFINED_UNCATEGORIZED_LABEL,
                        value = fact.expense,
                        currency = fact.currency,
                        referenceDate = selectedMonthEnd,
                    ),
                )
            }.toList()
    }
}
