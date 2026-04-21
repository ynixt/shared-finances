package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboard
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboardCharts
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboardMemberPie
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboardMemberSeries
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboardSeries
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDebtPair
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardChartPoint
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardPieSlice
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardScope
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
internal class GroupOverviewBuilderService(
    private val dataService: OverviewDashboardDataServiceImpl,
    private val goalService: OverviewDashboardGoalServiceImpl,
    private val balanceService: OverviewDashboardBalanceServiceImpl,
    private val contributionService: OverviewDashboardContributionServiceImpl,
    private val chartService: OverviewDashboardChartServiceImpl,
    private val assemblyService: OverviewDashboardAssemblyServiceImpl,
    private val clock: Clock,
) {
    suspend fun build(
        userId: UUID,
        groupId: UUID,
        defaultCurrency: String,
        selectedMonth: YearMonth,
    ): GroupOverviewDashboard {
        val targetCurrency = defaultCurrency.uppercase()
        val today = LocalDate.now(clock)
        val currentMonth = YearMonth.from(today)
        val selectedMonthEnd = selectedMonth.atEndOfMonth()
        val chartMonths = buildMonthRange(selectedMonth.minusMonths(11), selectedMonth)
        val chartMonthSet = chartMonths.toSet()
        val scope = OverviewDashboardScope.Group(actorUserId = userId, groupId = groupId)

        val visibleItems = dataService.loadVisibleItems(scope)
        val groupMembers = dataService.loadGroupMembers(actorUserId = userId, groupId = groupId)
        val memberNameById = buildMemberNameById(groupMembers).toMutableMap()
        val allMemberIds = mutableSetOf<UUID>()
        allMemberIds.addAll(groupMembers.map { it.userId })
        allMemberIds.addAll(visibleItems.items.map { it.userId })

        val executedEvents =
            dataService.loadExecutedEvents(
                scope = scope,
                minimumDate = chartMonths.first().atDay(1),
                maximumDate = minOf(today, selectedMonthEnd),
                entryTypes = setOf(WalletEntryType.REVENUE, WalletEntryType.EXPENSE),
            )
        val projectedEvents =
            if (selectedMonth.isBefore(currentMonth)) {
                emptyList()
            } else {
                dataService.loadProjectedEvents(
                    scope = scope,
                    minimumDate = today,
                    maximumDate = selectedMonthEnd,
                    visibleWalletItemIds = visibleItems.walletItemIds,
                    entryTypes = setOf(WalletEntryType.REVENUE, WalletEntryType.EXPENSE),
                )
            }

        registerMemberNamesFromEvents(executedEvents + projectedEvents, memberNameById, allMemberIds)
        if (allMemberIds.isEmpty()) {
            allMemberIds.add(userId)
        }

        val rawDetailByCardKey = linkedMapOf<OverviewDashboardCardKey, MutableList<RawDetail>>()
        val rawChartContributions = mutableListOf<RawChartContribution>()
        val rawChartContributionsByMember = mutableMapOf<UUID, MutableList<RawChartContribution>>()
        val rawCashInCategoryBreakdown = mutableListOf<RawBreakdownContribution>()
        val rawCashInCategoryBreakdownByMember = mutableMapOf<UUID, MutableList<RawBreakdownContribution>>()
        val rawExpenseCategoryBreakdown = mutableListOf<RawBreakdownContribution>()
        val rawExpenseCategoryBreakdownByMember = mutableMapOf<UUID, MutableList<RawBreakdownContribution>>()
        val rawExpenseByMemberBreakdown = mutableListOf<RawBreakdownContribution>()

        processEvents(
            events = executedEvents,
            component = ChartPointComponent.EXECUTED,
            selectedMonth = selectedMonth,
            selectedMonthEnd = selectedMonthEnd,
            chartMonthSet = chartMonthSet,
            visibleWalletItemIds = visibleItems.walletItemIds,
            memberNameById = memberNameById,
            rawDetailByCardKey = rawDetailByCardKey,
            rawChartContributions = rawChartContributions,
            rawChartContributionsByMember = rawChartContributionsByMember,
            rawCashInCategoryBreakdown = rawCashInCategoryBreakdown,
            rawCashInCategoryBreakdownByMember = rawCashInCategoryBreakdownByMember,
            rawExpenseCategoryBreakdown = rawExpenseCategoryBreakdown,
            rawExpenseCategoryBreakdownByMember = rawExpenseCategoryBreakdownByMember,
            rawExpenseByMemberBreakdown = rawExpenseByMemberBreakdown,
        )
        processEvents(
            events = projectedEvents,
            component = ChartPointComponent.PROJECTED,
            selectedMonth = selectedMonth,
            selectedMonthEnd = selectedMonthEnd,
            chartMonthSet = chartMonthSet,
            visibleWalletItemIds = visibleItems.walletItemIds,
            memberNameById = memberNameById,
            rawDetailByCardKey = rawDetailByCardKey,
            rawChartContributions = rawChartContributions,
            rawChartContributionsByMember = rawChartContributionsByMember,
            rawCashInCategoryBreakdown = rawCashInCategoryBreakdown,
            rawCashInCategoryBreakdownByMember = rawCashInCategoryBreakdownByMember,
            rawExpenseCategoryBreakdown = rawExpenseCategoryBreakdown,
            rawExpenseCategoryBreakdownByMember = rawExpenseCategoryBreakdownByMember,
            rawExpenseByMemberBreakdown = rawExpenseByMemberBreakdown,
        )

        val rawBalanceByBankId = visibleItems.bankAccounts.associate { bank -> bank.id!! to bank.balance.asMoney() }
        val goalCommitmentContext =
            goalService.loadGoalCommitmentContext(
                scope = scope,
                bankAccountIds = visibleItems.bankAccountIds,
                bankAccountById = visibleItems.bankAccountById,
                rawBalanceByBankId = rawBalanceByBankId,
                referenceDate = balanceService.balanceReferenceDateForMonth(selectedMonth, currentMonth, today),
            )
        val rawGoalCommittedDetails = goalCommitmentContext.rawDetails

        val rawBreakdownContributions =
            rawCashInCategoryBreakdown +
                rawCashInCategoryBreakdownByMember.values.flatten() +
                rawExpenseCategoryBreakdown +
                rawExpenseCategoryBreakdownByMember.values.flatten() +
                rawExpenseByMemberBreakdown

        val convertedValueByKey =
            assemblyService.convertRawValues(
                rawValues =
                    contributionService.collectRawValues(
                        rawDetailByCardKey = rawDetailByCardKey,
                        rawChartContributions = rawChartContributions + rawChartContributionsByMember.values.flatten(),
                        rawGoalCommittedDetails = rawGoalCommittedDetails,
                        rawBreakdownContributions = rawBreakdownContributions,
                    ),
                targetCurrency = targetCurrency,
            )

        val convertedDetailByCardKey = assemblyService.buildConvertedDetails(rawDetailByCardKey, convertedValueByKey)
        val goalCommittedTotal =
            rawGoalCommittedDetails
                .fold(BigDecimal.ZERO) { acc, raw -> acc.add(convertedValueByKey.getOrDefault(raw.key, BigDecimal.ZERO)) }
                .asMoney()
        val goalCommittedDetails =
            goalService.buildGoalCommittedDetailsByWallet(
                rawGoalCommittedDetails = rawGoalCommittedDetails,
                convertedValueByKey = convertedValueByKey,
                visibleBankAccounts = visibleItems.bankAccounts,
                rawBalanceByBankId = rawBalanceByBankId,
            )

        val sortedMembers = allMemberIds.map { it to resolveMemberName(it, memberNameById) }.sortedBy { (_, name) -> name.lowercase() }
        val debtPairs = buildDebtPairs(userId, groupId, selectedMonth, targetCurrency, memberNameById)

        return GroupOverviewDashboard(
            selectedMonth = selectedMonth,
            currency = targetCurrency,
            cards = buildCards(convertedDetailByCardKey, goalCommittedTotal, goalCommittedDetails, debtPairs),
            charts =
                buildCharts(
                    chartMonths = chartMonths,
                    sortedMembers = sortedMembers,
                    convertedValueByKey = convertedValueByKey,
                    rawChartContributions = rawChartContributions,
                    rawChartContributionsByMember = rawChartContributionsByMember,
                    rawCashInCategoryBreakdown = rawCashInCategoryBreakdown,
                    rawCashInCategoryBreakdownByMember = rawCashInCategoryBreakdownByMember,
                    rawExpenseCategoryBreakdown = rawExpenseCategoryBreakdown,
                    rawExpenseCategoryBreakdownByMember = rawExpenseCategoryBreakdownByMember,
                    rawExpenseByMemberBreakdown = rawExpenseByMemberBreakdown,
                ),
            debtPairs = debtPairs,
            goalOverCommittedWarning = goalCommitmentContext.hasAccountOverCommittedBalance,
        )
    }

    private fun processEvents(
        events: List<EventListResponse>,
        component: ChartPointComponent,
        selectedMonth: YearMonth,
        selectedMonthEnd: LocalDate,
        chartMonthSet: Set<YearMonth>,
        visibleWalletItemIds: Set<UUID>,
        memberNameById: Map<UUID, String>,
        rawDetailByCardKey: MutableMap<OverviewDashboardCardKey, MutableList<RawDetail>>,
        rawChartContributions: MutableList<RawChartContribution>,
        rawChartContributionsByMember: MutableMap<UUID, MutableList<RawChartContribution>>,
        rawCashInCategoryBreakdown: MutableList<RawBreakdownContribution>,
        rawCashInCategoryBreakdownByMember: MutableMap<UUID, MutableList<RawBreakdownContribution>>,
        rawExpenseCategoryBreakdown: MutableList<RawBreakdownContribution>,
        rawExpenseCategoryBreakdownByMember: MutableMap<UUID, MutableList<RawBreakdownContribution>>,
        rawExpenseByMemberBreakdown: MutableList<RawBreakdownContribution>,
    ) {
        events.forEach { event ->
            if (event.transferPurpose == TransferPurpose.DEBT_SETTLEMENT) {
                return@forEach
            }

            if (event.type != WalletEntryType.REVENUE && event.type != WalletEntryType.EXPENSE) {
                return@forEach
            }

            val month = YearMonth.from(event.date)
            val inChartRange = chartMonthSet.contains(month)
            val inSelectedMonth = month == selectedMonth

            event.entries.forEach { entry ->
                if (!visibleWalletItemIds.contains(entry.walletItemId)) {
                    return@forEach
                }

                val walletItem = entry.walletItem
                val memberId = walletItem.userId
                val memberName = resolveMemberName(memberId, memberNameById)

                when (event.type) {
                    WalletEntryType.REVENUE -> {
                        if (inChartRange) {
                            addChartContribution(
                                month = month,
                                value = entry.value,
                                currency = walletItem.currency,
                                component = component,
                                memberId = memberId,
                                allContributions = rawChartContributions,
                                memberContributions = rawChartContributionsByMember,
                                series = ChartSeries.CASH_IN,
                            )
                        }

                        if (inSelectedMonth) {
                            val cardKey =
                                if (component == ChartPointComponent.EXECUTED) {
                                    OverviewDashboardCardKey.PERIOD_CASH_IN
                                } else {
                                    OverviewDashboardCardKey.PROJECTED_CASH_IN
                                }
                            rawDetailByCardKey
                                .getOrPut(cardKey) { mutableListOf() }
                                .add(
                                    RawDetail(
                                        sourceId = entry.walletItemId,
                                        sourceType = detailSourceTypeForWalletItemType(walletItem.type),
                                        label = walletItem.name,
                                        value = entry.value.asMoney(),
                                        currency = walletItem.currency.uppercase(),
                                        referenceDate = selectedMonthEnd,
                                    ),
                                )

                            val categoryId = event.category?.id
                            val categoryLabel = event.category?.name ?: PREDEFINED_UNCATEGORIZED_LABEL
                            addBreakdownContribution(
                                breakdownType = BreakdownType.CASH_IN_CATEGORY,
                                component = component,
                                value = entry.value.asMoney(),
                                currency = walletItem.currency,
                                sliceId = categoryId,
                                label = categoryLabel,
                                referenceDate = selectedMonthEnd,
                                memberId = memberId,
                                allBreakdowns = rawCashInCategoryBreakdown,
                                memberBreakdowns = rawCashInCategoryBreakdownByMember,
                            )
                        }
                    }

                    WalletEntryType.EXPENSE -> {
                        val expenseValue = entry.value.abs().asMoney()
                        if (inChartRange) {
                            addChartContribution(
                                month = month,
                                value = expenseValue,
                                currency = walletItem.currency,
                                component = component,
                                memberId = memberId,
                                allContributions = rawChartContributions,
                                memberContributions = rawChartContributionsByMember,
                                series = ChartSeries.EXPENSE,
                            )
                        }

                        if (inSelectedMonth) {
                            val cardKey =
                                if (component == ChartPointComponent.EXECUTED) {
                                    OverviewDashboardCardKey.PERIOD_EXPENSES
                                } else {
                                    OverviewDashboardCardKey.PROJECTED_EXPENSES
                                }
                            rawDetailByCardKey
                                .getOrPut(cardKey) { mutableListOf() }
                                .add(
                                    RawDetail(
                                        sourceId = entry.walletItemId,
                                        sourceType = detailSourceTypeForWalletItemType(walletItem.type),
                                        label = walletItem.name,
                                        value = expenseValue,
                                        currency = walletItem.currency.uppercase(),
                                        referenceDate = selectedMonthEnd,
                                    ),
                                )

                            val categoryId = event.category?.id
                            val categoryLabel = event.category?.name ?: PREDEFINED_UNCATEGORIZED_LABEL
                            addBreakdownContribution(
                                breakdownType = BreakdownType.EXPENSE_CATEGORY,
                                component = component,
                                value = expenseValue,
                                currency = walletItem.currency,
                                sliceId = categoryId,
                                label = categoryLabel,
                                referenceDate = selectedMonthEnd,
                                memberId = memberId,
                                allBreakdowns = rawExpenseCategoryBreakdown,
                                memberBreakdowns = rawExpenseCategoryBreakdownByMember,
                            )
                            rawExpenseByMemberBreakdown.add(
                                RawBreakdownContribution(
                                    breakdownType = BreakdownType.EXPENSE_GROUP,
                                    component = component,
                                    sliceId = memberId,
                                    label = memberName,
                                    value = expenseValue,
                                    currency = walletItem.currency.uppercase(),
                                    referenceDate = selectedMonthEnd,
                                ),
                            )
                        }
                    }

                }
            }
        }
    }

    private fun buildCards(
        convertedDetailByCardKey: Map<OverviewDashboardCardKey, List<OverviewDashboardDetail>>,
        goalCommittedTotal: BigDecimal,
        goalCommittedDetails: List<OverviewDashboardDetail>,
        debtPairs: List<GroupOverviewDebtPair>,
    ): List<OverviewDashboardCard> {
        val periodCashInTotal = convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_IN].orEmpty().sumByValue()
        val periodExpensesTotal = convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_EXPENSES].orEmpty().sumByValue()
        val projectedCashInTotal = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_IN].orEmpty().sumByValue()
        val projectedExpensesTotal = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_EXPENSES].orEmpty().sumByValue()
        val debtDetails =
            debtPairs
                .map { pair ->
                    OverviewDashboardDetail(
                        sourceId = null,
                        sourceType = OverviewDashboardDetailSourceType.GROUP_DEBT_PAIR,
                        label = "${pair.payerName} -> ${pair.receiverName}",
                        value = pair.outstandingAmount,
                        children = pair.details,
                    )
                }.sortedWith(compareByDescending<OverviewDashboardDetail> { it.value }.thenBy { it.label.lowercase() })
        val debtTotal = debtPairs.fold(BigDecimal.ZERO) { acc, pair -> acc.add(pair.outstandingAmount) }.asMoney()

        return listOf(
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.GOAL_COMMITTED,
                value = goalCommittedTotal,
                details = goalCommittedDetails,
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PERIOD_CASH_IN,
                value = periodCashInTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_IN].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PERIOD_EXPENSES,
                value = periodExpensesTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_EXPENSES].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PROJECTED_CASH_IN,
                value = projectedCashInTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_IN].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PROJECTED_EXPENSES,
                value = projectedExpensesTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_EXPENSES].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.GROUP_MEMBER_DEBTS,
                value = debtTotal,
                details = debtDetails,
            ),
        )
    }

    private fun buildCharts(
        chartMonths: List<YearMonth>,
        sortedMembers: List<Pair<UUID, String>>,
        convertedValueByKey: Map<String, BigDecimal>,
        rawChartContributions: List<RawChartContribution>,
        rawChartContributionsByMember: Map<UUID, List<RawChartContribution>>,
        rawCashInCategoryBreakdown: List<RawBreakdownContribution>,
        rawCashInCategoryBreakdownByMember: Map<UUID, List<RawBreakdownContribution>>,
        rawExpenseCategoryBreakdown: List<RawBreakdownContribution>,
        rawExpenseCategoryBreakdownByMember: Map<UUID, List<RawBreakdownContribution>>,
        rawExpenseByMemberBreakdown: List<RawBreakdownContribution>,
    ): GroupOverviewDashboardCharts {
        val totalChartValues = chartService.accumulateChartValues(rawChartContributions, convertedValueByKey)
        val cashInTotalSeries = buildChartPoints(chartMonths, ChartSeries.CASH_IN, totalChartValues)
        val expenseTotalSeries = buildChartPoints(chartMonths, ChartSeries.EXPENSE, totalChartValues)

        val cashInByMember =
            sortedMembers.map { (memberId, memberName) ->
                val chartValues =
                    chartService.accumulateChartValues(
                        rawChartContributionsByMember[memberId].orEmpty(),
                        convertedValueByKey,
                    )
                GroupOverviewDashboardMemberSeries(
                    memberId = memberId,
                    memberName = memberName,
                    points = buildChartPoints(chartMonths, ChartSeries.CASH_IN, chartValues),
                )
            }

        val expenseByMember =
            sortedMembers.map { (memberId, memberName) ->
                val chartValues =
                    chartService.accumulateChartValues(
                        rawChartContributionsByMember[memberId].orEmpty(),
                        convertedValueByKey,
                    )
                GroupOverviewDashboardMemberSeries(
                    memberId = memberId,
                    memberName = memberName,
                    points = buildChartPoints(chartMonths, ChartSeries.EXPENSE, chartValues),
                )
            }

        val cashInByCategoryTotal =
            chartService.buildPieSlices(
                breakdownValueByKey = chartService.accumulateBreakdownValues(rawCashInCategoryBreakdown, convertedValueByKey),
                breakdownType = BreakdownType.CASH_IN_CATEGORY,
                alwaysIncludeLabel = PREDEFINED_UNCATEGORIZED_LABEL,
            )
        val cashInByCategoryByMember =
            sortedMembers.map { (memberId, memberName) ->
                GroupOverviewDashboardMemberPie(
                    memberId = memberId,
                    memberName = memberName,
                    slices =
                        chartService.buildPieSlices(
                            breakdownValueByKey =
                                chartService.accumulateBreakdownValues(
                                    rawCashInCategoryBreakdownByMember[memberId].orEmpty(),
                                    convertedValueByKey,
                                ),
                            breakdownType = BreakdownType.CASH_IN_CATEGORY,
                            alwaysIncludeLabel = PREDEFINED_UNCATEGORIZED_LABEL,
                        ),
                )
            }
        val expenseByCategory =
            chartService.buildPieSlices(
                breakdownValueByKey = chartService.accumulateBreakdownValues(rawExpenseCategoryBreakdown, convertedValueByKey),
                breakdownType = BreakdownType.EXPENSE_CATEGORY,
                alwaysIncludeLabel = PREDEFINED_UNCATEGORIZED_LABEL,
            )
        val expenseByCategoryByMember =
            sortedMembers.map { (memberId, memberName) ->
                GroupOverviewDashboardMemberPie(
                    memberId = memberId,
                    memberName = memberName,
                    slices =
                        chartService.buildPieSlices(
                            breakdownValueByKey =
                                chartService.accumulateBreakdownValues(
                                    rawExpenseCategoryBreakdownByMember[memberId].orEmpty(),
                                    convertedValueByKey,
                                ),
                            breakdownType = BreakdownType.EXPENSE_CATEGORY,
                            alwaysIncludeLabel = PREDEFINED_UNCATEGORIZED_LABEL,
                        ),
                )
            }
        val expenseByMemberPie =
            chartService
                .accumulateBreakdownValues(rawExpenseByMemberBreakdown, convertedValueByKey)
                .entries
                .asSequence()
                .filter { (key, _) -> key.breakdownType == BreakdownType.EXPENSE_GROUP }
                .map { (key, components) ->
                    OverviewDashboardPieSlice(
                        id = key.sliceId,
                        label = key.label,
                        value = components.total().asMoney(),
                        executedValue = components.executed.asMoney(),
                        projectedValue = components.projected.asMoney(),
                    )
                }.filter { slice -> slice.value.compareTo(BigDecimal.ZERO) > 0 }
                .sortedWith(compareByDescending<OverviewDashboardPieSlice> { it.value }.thenBy { it.label })
                .toList()

        return GroupOverviewDashboardCharts(
            cashIn =
                GroupOverviewDashboardSeries(
                    total = cashInTotalSeries,
                    byMember = cashInByMember,
                ),
            expense =
                GroupOverviewDashboardSeries(
                    total = expenseTotalSeries,
                    byMember = expenseByMember,
                ),
            cashInByCategoryTotal = cashInByCategoryTotal,
            cashInByCategoryByMember = cashInByCategoryByMember,
            expenseByCategory = expenseByCategory,
            expenseByCategoryByMember = expenseByCategoryByMember,
            expenseByMember = expenseByMemberPie,
        )
    }

    private suspend fun buildDebtPairs(
        actorUserId: UUID,
        groupId: UUID,
        selectedMonth: YearMonth,
        targetCurrency: String,
        memberNameById: Map<UUID, String>,
    ): List<GroupOverviewDebtPair> {
        val workspace = dataService.loadGroupDebtWorkspace(actorUserId = actorUserId, groupId = groupId)
        if (workspace.balances.isEmpty()) {
            return emptyList()
        }

        val directionalTotals = mutableMapOf<DirectionalDebtKey, BigDecimal>()
        val directionalByMonth = mutableMapOf<Pair<DirectionalDebtKey, YearMonth>, BigDecimal>()

        workspace.balances.forEach { balance ->
            val currency = balance.currency.uppercase()
            balance.monthlyComposition
                .asSequence()
                .filter { composition -> !composition.month.isAfter(selectedMonth) }
                .forEach { composition ->
                    val signed = composition.netAmount.asMoney()
                    if (signed.compareTo(BigDecimal.ZERO) == 0) {
                        return@forEach
                    }

                    val (direction, amount) =
                        if (signed.compareTo(BigDecimal.ZERO) > 0) {
                            DirectionalDebtKey(balance.payerId, balance.receiverId, currency) to signed
                        } else {
                            DirectionalDebtKey(balance.receiverId, balance.payerId, currency) to signed.abs()
                        }

                    directionalTotals[direction] =
                        directionalTotals.getOrDefault(direction, BigDecimal.ZERO).add(amount).asMoney()

                    val monthKey = direction to composition.month
                    directionalByMonth[monthKey] =
                        directionalByMonth
                            .getOrDefault(monthKey, BigDecimal.ZERO)
                            .add(amount)
                            .asMoney()
                }
        }

        if (directionalTotals.isEmpty()) {
            return emptyList()
        }

        val rawPairs =
            directionalTotals.keys
                .map { key -> canonicalDebtKeyFor(key.payerId, key.receiverId, key.currency) }
                .toSet()
                .mapNotNull { canonical ->
                    val forward = DirectionalDebtKey(canonical.firstUserId, canonical.secondUserId, canonical.currency)
                    val backward = DirectionalDebtKey(canonical.secondUserId, canonical.firstUserId, canonical.currency)

                    val forwardAmount = directionalTotals.getOrDefault(forward, BigDecimal.ZERO).asMoney()
                    val backwardAmount = directionalTotals.getOrDefault(backward, BigDecimal.ZERO).asMoney()
                    val net = forwardAmount.subtract(backwardAmount).asMoney()
                    if (net.compareTo(BigDecimal.ZERO) == 0) {
                        return@mapNotNull null
                    }

                    val finalDirection = if (net.compareTo(BigDecimal.ZERO) > 0) forward else backward
                    val outstanding = net.abs().asMoney()
                    val minimumMonth = canonical.minimumMonth(directionalByMonth)
                    val monthValues =
                        buildMonthRange(minimumMonth, selectedMonth)
                            .mapNotNull { month ->
                                val monthForward = directionalByMonth.getOrDefault(forward to month, BigDecimal.ZERO).asMoney()
                                val monthBackward = directionalByMonth.getOrDefault(backward to month, BigDecimal.ZERO).asMoney()
                                val monthNet =
                                    if (finalDirection == forward) {
                                        monthForward.subtract(monthBackward)
                                    } else {
                                        monthBackward.subtract(monthForward)
                                    }.asMoney()

                                if (monthNet.compareTo(BigDecimal.ZERO) <= 0) {
                                    null
                                } else {
                                    GroupDebtMonthRaw(month = month, value = monthNet)
                                }
                            }

                    GroupDebtPairRaw(
                        payerId = finalDirection.payerId,
                        receiverId = finalDirection.receiverId,
                        currency = finalDirection.currency,
                        outstandingAmount = outstanding,
                        monthValues = monthValues,
                    )
                }.filter { pair -> pair.outstandingAmount.compareTo(BigDecimal.ZERO) > 0 }

        if (rawPairs.isEmpty()) {
            return emptyList()
        }

        val rawValues = mutableListOf<RawValue>()
        val pairAmountKeyByIndex = mutableMapOf<Int, String>()
        val pairMonthKeyByIndexAndMonth = mutableMapOf<Pair<Int, YearMonth>, String>()

        rawPairs.forEachIndexed { index, pair ->
            val amountKey = "group-debt-pair-$index"
            pairAmountKeyByIndex[index] = amountKey
            rawValues.add(
                RawValue(
                    key = amountKey,
                    value = pair.outstandingAmount,
                    currency = pair.currency,
                    referenceDate = selectedMonth.atEndOfMonth(),
                ),
            )

            pair.monthValues.forEach { monthRaw ->
                val monthKey = "group-debt-pair-$index-${monthRaw.month}"
                pairMonthKeyByIndexAndMonth[index to monthRaw.month] = monthKey
                rawValues.add(
                    RawValue(
                        key = monthKey,
                        value = monthRaw.value,
                        currency = pair.currency,
                        referenceDate = monthRaw.month.atEndOfMonth(),
                    ),
                )
            }
        }

        val converted = assemblyService.convertRawValues(rawValues = rawValues, targetCurrency = targetCurrency)

        return rawPairs
            .mapIndexed { index, pair ->
                GroupOverviewDebtPair(
                    payerId = pair.payerId,
                    payerName = resolveMemberName(pair.payerId, memberNameById),
                    receiverId = pair.receiverId,
                    receiverName = resolveMemberName(pair.receiverId, memberNameById),
                    currency = targetCurrency,
                    outstandingAmount = converted.getOrDefault(pairAmountKeyByIndex.getValue(index), BigDecimal.ZERO).asMoney(),
                    details =
                        pair.monthValues
                            .mapNotNull { monthRaw ->
                                val key = pairMonthKeyByIndexAndMonth[index to monthRaw.month] ?: return@mapNotNull null
                                val value = converted.getOrDefault(key, BigDecimal.ZERO).asMoney()
                                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                                    null
                                } else {
                                    OverviewDashboardDetail(
                                        sourceId = null,
                                        sourceType = OverviewDashboardDetailSourceType.FORMULA,
                                        label = monthRaw.month.format(monthFormatter),
                                        value = value,
                                    )
                                }
                            }.sortedBy { detail -> detail.label },
                )
            }.filter { pair -> pair.outstandingAmount.compareTo(BigDecimal.ZERO) > 0 }
            .sortedWith(
                compareByDescending<GroupOverviewDebtPair> { it.outstandingAmount }
                    .thenBy { it.payerName.lowercase() }
                    .thenBy { it.receiverName.lowercase() },
            )
    }

    private fun addChartContribution(
        month: YearMonth,
        value: BigDecimal,
        currency: String,
        component: ChartPointComponent,
        memberId: UUID,
        allContributions: MutableList<RawChartContribution>,
        memberContributions: MutableMap<UUID, MutableList<RawChartContribution>>,
        series: ChartSeries,
    ) {
        val contribution =
            RawChartContribution(
                chartSeries = series,
                component = component,
                month = month,
                value = value.asMoney(),
                currency = currency.uppercase(),
                referenceDate = month.atEndOfMonth(),
            )
        allContributions.add(contribution)
        memberContributions.getOrPut(memberId) { mutableListOf() }.add(contribution)
    }

    private fun addBreakdownContribution(
        breakdownType: BreakdownType,
        component: ChartPointComponent,
        value: BigDecimal,
        currency: String,
        sliceId: UUID?,
        label: String,
        referenceDate: LocalDate,
        memberId: UUID,
        allBreakdowns: MutableList<RawBreakdownContribution>,
        memberBreakdowns: MutableMap<UUID, MutableList<RawBreakdownContribution>>,
    ) {
        val contribution =
            RawBreakdownContribution(
                breakdownType = breakdownType,
                component = component,
                sliceId = sliceId,
                label = label,
                value = value.asMoney(),
                currency = currency.uppercase(),
                referenceDate = referenceDate,
            )
        allBreakdowns.add(contribution)
        memberBreakdowns.getOrPut(memberId) { mutableListOf() }.add(contribution)
    }

    private fun buildChartPoints(
        chartMonths: List<YearMonth>,
        chartSeries: ChartSeries,
        chartValuesBySeriesAndMonth: Map<Pair<ChartSeries, YearMonth>, ChartValueComponents>,
    ): List<OverviewDashboardChartPoint> =
        chartMonths.map { month ->
            val value = chartValuesBySeriesAndMonth.getOrDefault(chartSeries to month, ChartValueComponents.ZERO)
            OverviewDashboardChartPoint(
                month = month,
                value = value.total().asMoney(),
                executedValue = value.executed.asMoney(),
                projectedValue = value.projected.asMoney(),
            )
        }

    private fun buildMemberNameById(groupMembers: List<GroupUserEntity>): Map<UUID, String> =
        groupMembers.associate { member ->
            member.userId to userDisplayName(member)
        }

    private fun registerMemberNamesFromEvents(
        events: List<EventListResponse>,
        memberNameById: MutableMap<UUID, String>,
        allMemberIds: MutableSet<UUID>,
    ) {
        events
            .asSequence()
            .flatMap { event -> event.entries.asSequence() }
            .forEach { entry ->
                val memberId = entry.walletItem.userId
                allMemberIds.add(memberId)
                val firstName =
                    entry.walletItem.user
                        ?.firstName
                        ?.trim()
                        .orEmpty()
                val lastName =
                    entry.walletItem.user
                        ?.lastName
                        ?.trim()
                        .orEmpty()
                val fullName =
                    listOf(firstName, lastName)
                        .filter { name -> name.isNotBlank() }
                        .joinToString(" ")
                        .trim()
                if (fullName.isNotBlank()) {
                    memberNameById.putIfAbsent(memberId, fullName)
                }
            }
    }

    private fun userDisplayName(member: GroupUserEntity): String {
        val firstName =
            member.user
                ?.firstName
                ?.trim()
                .orEmpty()
        val lastName =
            member.user
                ?.lastName
                ?.trim()
                .orEmpty()
        val fullName =
            listOf(firstName, lastName)
                .filter { value -> value.isNotBlank() }
                .joinToString(" ")
                .trim()
        if (fullName.isNotBlank()) {
            return fullName
        }
        return "Member ${member.userId.toString().take(8)}"
    }

    private fun resolveMemberName(
        memberId: UUID,
        memberNameById: Map<UUID, String>,
    ): String = memberNameById[memberId] ?: "Member ${memberId.toString().take(8)}"

    private fun canonicalDebtKeyFor(
        userA: UUID,
        userB: UUID,
        currency: String,
    ): CanonicalDebtKey =
        if (userA.toString() <= userB.toString()) {
            CanonicalDebtKey(firstUserId = userA, secondUserId = userB, currency = currency.uppercase())
        } else {
            CanonicalDebtKey(firstUserId = userB, secondUserId = userA, currency = currency.uppercase())
        }

    private fun CanonicalDebtKey.minimumMonth(directionalByMonth: Map<Pair<DirectionalDebtKey, YearMonth>, BigDecimal>): YearMonth {
        val forward = DirectionalDebtKey(firstUserId, secondUserId, currency)
        val backward = DirectionalDebtKey(secondUserId, firstUserId, currency)
        return directionalByMonth.keys
            .asSequence()
            .filter { (direction, _) -> direction == forward || direction == backward }
            .map { (_, month) -> month }
            .minOrNull()
            ?: YearMonth.now(clock)
    }

    private fun List<OverviewDashboardDetail>.sumByValue(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, detail -> acc.add(detail.value) }.asMoney()

    private data class DirectionalDebtKey(
        val payerId: UUID,
        val receiverId: UUID,
        val currency: String,
    )

    private data class CanonicalDebtKey(
        val firstUserId: UUID,
        val secondUserId: UUID,
        val currency: String,
    )

    private data class GroupDebtMonthRaw(
        val month: YearMonth,
        val value: BigDecimal,
    )

    private data class GroupDebtPairRaw(
        val payerId: UUID,
        val receiverId: UUID,
        val currency: String,
        val outstandingAmount: BigDecimal,
        val monthValues: List<GroupDebtMonthRaw>,
    )

    companion object {
        private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-yyyy")
    }
}
