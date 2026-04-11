package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashDirection
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardChartPoint
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCharts
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardPieSlice
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.dashboard.OverviewDashboardService
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class OverviewDashboardServiceImpl(
    private val walletItemRepository: WalletItemRepository,
    private val walletItemMapper: WalletItemMapper,
    private val walletEntryRepository: WalletEntryRepository,
    private val recurrenceSimulationService: RecurrenceSimulationService,
    private val creditCardBillService: CreditCardBillService,
    private val exchangeRateService: ExchangeRateService,
    private val clock: Clock,
) : OverviewDashboardService {
    override suspend fun getOverview(
        userId: UUID,
        defaultCurrency: String,
        selectedMonth: YearMonth,
    ): OverviewDashboard {
        val normalizedTargetCurrency = defaultCurrency.uppercase()
        val today = LocalDate.now(clock)
        val currentMonth = YearMonth.from(today)
        val selectedMonthStart = selectedMonth.atDay(1)
        val selectedMonthEnd = selectedMonth.atEndOfMonth()
        val chartStartMonth = selectedMonth.minusMonths(11)
        val chartMonths = buildMonthRange(chartStartMonth, selectedMonth)

        val visibleWalletItems =
            walletItemRepository
                .findAllByUserIdAndEnabled(
                    userId = userId,
                    enabled = true,
                    pageable = Pageable.unpaged(),
                ).map(walletItemMapper::toModel)
                .collectList()
                .awaitSingle()
                .filter(WalletItem::showOnDashboard)

        val visibleBankAccounts = visibleWalletItems.filterIsInstance<BankAccount>()
        val visibleCreditCards = visibleWalletItems.filterIsInstance<CreditCard>()
        val bankAccountById = visibleBankAccounts.associateBy { it.id!! }
        val bankAccountIds = bankAccountById.keys
        val maximumExecutedDate = minOf(today, selectedMonthEnd)

        val executedByMonthByBankId =
            fetchExecutedByMonthByBank(
                userId = userId,
                minimumDate = chartStartMonth.atDay(1),
                maximumDate = maximumExecutedDate,
            )

        val projectedByMonthByBankId =
            fetchProjectedByMonthByBank(
                userId = userId,
                minimumDate = today.plusDays(1),
                maximumDate = selectedMonthEnd,
                visibleBankAccountIds = bankAccountIds,
            )

        val projectedCreditCardDetails =
            fetchProjectedCreditCardCashOut(
                userId = userId,
                selectedMonth = selectedMonth,
                creditCards = visibleCreditCards,
            )

        val rawExpenseChartContributions =
            fetchExecutedExpenseByMonth(
                userId = userId,
                minimumDate = chartStartMonth.atDay(1),
                maximumDate = maximumExecutedDate,
            ).map { summary ->
                RawChartContribution(
                    chartSeries = ChartSeries.EXPENSE,
                    month = summary.month,
                    value = summary.expense,
                    currency = summary.currency,
                    referenceDate = summary.month.atEndOfMonth(),
                )
            }

        val rawBreakdownContributions =
            walletEntryRepository
                .summarizeOverviewCashBreakdown(
                    userId = userId,
                    minimumDate = selectedMonthStart,
                    maximumDate = selectedMonthEnd,
                ).collectList()
                .awaitSingle()
                .map { summary ->
                    RawBreakdownContribution(
                        breakdownType =
                            when (summary.direction) {
                                OverviewCashDirection.IN -> BreakdownType.CASH_IN_CATEGORY
                                OverviewCashDirection.OUT -> BreakdownType.CASH_OUT_CATEGORY
                            },
                        sliceId = summary.categoryId,
                        label = summary.categoryName ?: PREDEFINED_UNCATEGORIZED_LABEL,
                        value = summary.amount,
                        currency = summary.currency,
                        referenceDate = selectedMonthEnd,
                    )
                } +
                walletEntryRepository
                    .summarizeOverviewExpenseBreakdown(
                        userId = userId,
                        minimumDate = selectedMonthStart,
                        maximumDate = selectedMonthEnd,
                    ).collectList()
                    .awaitSingle()
                    .flatMap { summary ->
                        listOf(
                            RawBreakdownContribution(
                                breakdownType = BreakdownType.EXPENSE_GROUP,
                                sliceId = summary.groupId,
                                label = summary.groupName ?: PREDEFINED_INDIVIDUAL_LABEL,
                                value = summary.expense,
                                currency = summary.currency,
                                referenceDate = selectedMonthEnd,
                            ),
                            RawBreakdownContribution(
                                breakdownType = BreakdownType.EXPENSE_CATEGORY,
                                sliceId = summary.categoryId,
                                label = summary.categoryName ?: PREDEFINED_UNCATEGORIZED_LABEL,
                                value = summary.expense,
                                currency = summary.currency,
                                referenceDate = selectedMonthEnd,
                            ),
                        )
                    }

        val rawDetailByCardKey = linkedMapOf<OverviewDashboardCardKey, MutableList<RawDetail>>()

        fun addRawDetail(
            cardKey: OverviewDashboardCardKey,
            detail: RawDetail,
        ) {
            rawDetailByCardKey.getOrPut(cardKey) { mutableListOf() }.add(detail)
        }

        visibleBankAccounts.forEach { bankAccount ->
            val bankId = bankAccount.id!!
            val selectedMonthBalance =
                calculateBalanceForMonth(
                    bankAccount = bankAccount,
                    month = selectedMonth,
                    currentMonth = currentMonth,
                    executedByMonthByBankId = executedByMonthByBankId,
                    projectedByMonthByBankId = projectedByMonthByBankId,
                )
            val selectedMonthBalanceReferenceDate = balanceReferenceDateForMonth(selectedMonth, currentMonth, today)

            val selectedExecuted = executedByMonthByBankId.getMonthAmount(selectedMonth, bankId)
            val selectedProjected = projectedByMonthByBankId.getMonthAmount(selectedMonth, bankId)

            addRawDetail(
                OverviewDashboardCardKey.BALANCE,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedMonthBalance,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthBalanceReferenceDate,
                ),
            )

            addRawDetail(
                OverviewDashboardCardKey.PERIOD_CASH_IN,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedExecuted.cashIn,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )

            addRawDetail(
                OverviewDashboardCardKey.PERIOD_CASH_OUT,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedExecuted.cashOut,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )

            addRawDetail(
                OverviewDashboardCardKey.PROJECTED_CASH_IN,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedProjected.cashIn,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )

            addRawDetail(
                OverviewDashboardCardKey.PROJECTED_CASH_OUT,
                RawDetail(
                    sourceId = bankId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bankAccount.name,
                    value = selectedProjected.cashOut,
                    currency = bankAccount.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
        }

        projectedCreditCardDetails.forEach { creditCardProjected ->
            addRawDetail(
                OverviewDashboardCardKey.PROJECTED_CASH_OUT,
                RawDetail(
                    sourceId = creditCardProjected.creditCardId,
                    sourceType = OverviewDashboardDetailSourceType.CREDIT_CARD_BILL,
                    label = creditCardProjected.creditCardName,
                    value = creditCardProjected.projectedExpense,
                    currency = creditCardProjected.currency,
                    referenceDate = selectedMonthEnd,
                ),
            )
        }

        val rawChartContributions = mutableListOf<RawChartContribution>()
        chartMonths.forEach { month ->
            val monthEnd = month.atEndOfMonth()

            visibleBankAccounts.forEach { bankAccount ->
                val bankId = bankAccount.id!!
                val executed = executedByMonthByBankId.getMonthAmount(month, bankId)
                val balance =
                    calculateBalanceForMonth(
                        bankAccount = bankAccount,
                        month = month,
                        currentMonth = currentMonth,
                        executedByMonthByBankId = executedByMonthByBankId,
                        projectedByMonthByBankId = projectedByMonthByBankId,
                    )
                val balanceReferenceDate = balanceReferenceDateForMonth(month, currentMonth, today)

                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.BALANCE,
                        month = month,
                        value = balance,
                        currency = bankAccount.currency,
                        referenceDate = balanceReferenceDate,
                    ),
                )

                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.CASH_IN,
                        month = month,
                        value = executed.cashIn,
                        currency = bankAccount.currency,
                        referenceDate = monthEnd,
                    ),
                )

                rawChartContributions.add(
                    RawChartContribution(
                        chartSeries = ChartSeries.CASH_OUT,
                        month = month,
                        value = executed.cashOut,
                        currency = bankAccount.currency,
                        referenceDate = monthEnd,
                    ),
                )
            }
        }
        rawChartContributions.addAll(rawExpenseChartContributions)

        val rawValues = mutableListOf<RawValue>()
        rawDetailByCardKey.values.flatten().forEach { rawDetail ->
            rawValues.add(
                RawValue(
                    key = rawDetail.key,
                    value = rawDetail.value,
                    currency = rawDetail.currency,
                    referenceDate = rawDetail.referenceDate,
                ),
            )
        }
        rawChartContributions.forEach { rawChartContribution ->
            rawValues.add(
                RawValue(
                    key = rawChartContribution.key,
                    value = rawChartContribution.value,
                    currency = rawChartContribution.currency,
                    referenceDate = rawChartContribution.referenceDate,
                ),
            )
        }
        rawBreakdownContributions.forEach { rawBreakdownContribution ->
            rawValues.add(
                RawValue(
                    key = rawBreakdownContribution.key,
                    value = rawBreakdownContribution.value,
                    currency = rawBreakdownContribution.currency,
                    referenceDate = rawBreakdownContribution.referenceDate,
                ),
            )
        }

        val convertedValueByKey = convertRawValues(rawValues, normalizedTargetCurrency)

        val convertedDetailByCardKey =
            rawDetailByCardKey.mapValues { (_, details) ->
                details.map { detail ->
                    OverviewDashboardDetail(
                        sourceId = detail.sourceId,
                        sourceType = detail.sourceType,
                        label = detail.label,
                        value = convertedValueByKey.getOrDefault(detail.key, BigDecimal.ZERO).asMoney(),
                    )
                }
            }

        val balanceTotal = sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.BALANCE])
        val periodCashInTotal = sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_IN])
        val periodCashOutTotal = sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_OUT])
        val projectedCashInTotal = sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_IN])
        val projectedCashOutTotal = sumDetails(convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_OUT])

        val periodNetCashFlowTotal = (periodCashInTotal - periodCashOutTotal).asMoney()
        val projectedNetCashFlowTotal = (projectedCashInTotal - projectedCashOutTotal).asMoney()
        val endOfPeriodBalanceTotal = (balanceTotal + projectedNetCashFlowTotal).asMoney()
        val endOfPeriodNetCashFlowTotal = (endOfPeriodBalanceTotal - balanceTotal).asMoney()

        val periodNetCashFlowDetails =
            listOf(
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.periodCashIn",
                    value = periodCashInTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.periodCashOut",
                    value = periodCashOutTotal.negate().asMoney(),
                ),
            )

        val endOfPeriodBalanceDetails =
            listOf(
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.balance",
                    value = balanceTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.projectedCashIn",
                    value = projectedCashInTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.projectedCashOut",
                    value = projectedCashOutTotal.negate().asMoney(),
                ),
            )

        val endOfPeriodNetCashFlowDetails =
            listOf(
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.endOfPeriodBalance",
                    value = endOfPeriodBalanceTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.balance",
                    value = balanceTotal.negate().asMoney(),
                ),
            )

        val chartValueBySeriesAndMonth = linkedMapOf<Pair<ChartSeries, YearMonth>, BigDecimal>()
        rawChartContributions.forEach { rawChartContribution ->
            val key = rawChartContribution.chartSeries to rawChartContribution.month
            chartValueBySeriesAndMonth[key] =
                chartValueBySeriesAndMonth
                    .getOrDefault(key, BigDecimal.ZERO)
                    .add(convertedValueByKey.getOrDefault(rawChartContribution.key, BigDecimal.ZERO))
        }

        val breakdownValueByKey = linkedMapOf<BreakdownSliceKey, BigDecimal>()
        rawBreakdownContributions.forEach { rawBreakdownContribution ->
            val key =
                BreakdownSliceKey(
                    breakdownType = rawBreakdownContribution.breakdownType,
                    sliceId = rawBreakdownContribution.sliceId,
                    label = rawBreakdownContribution.label,
                )
            breakdownValueByKey[key] =
                breakdownValueByKey
                    .getOrDefault(key, BigDecimal.ZERO)
                    .add(convertedValueByKey.getOrDefault(rawBreakdownContribution.key, BigDecimal.ZERO))
        }

        val expenseByGroup =
            buildPieSlices(
                slices =
                    breakdownValueByKey.entries
                        .filter { it.key.breakdownType == BreakdownType.EXPENSE_GROUP }
                        .map { (key, value) ->
                            OverviewDashboardPieSlice(
                                id = key.sliceId,
                                label = key.label,
                                value = value.asMoney(),
                            )
                        },
                alwaysIncludeLabel = PREDEFINED_INDIVIDUAL_LABEL,
            )
        val expenseByCategory =
            buildPieSlices(
                slices =
                    breakdownValueByKey.entries
                        .filter { it.key.breakdownType == BreakdownType.EXPENSE_CATEGORY }
                        .map { (key, value) ->
                            OverviewDashboardPieSlice(
                                id = key.sliceId,
                                label = key.label,
                                value = value.asMoney(),
                            )
                        },
                alwaysIncludeLabel = PREDEFINED_UNCATEGORIZED_LABEL,
            )
        val cashInByCategory =
            buildPieSlices(
                slices =
                    breakdownValueByKey.entries
                        .filter { it.key.breakdownType == BreakdownType.CASH_IN_CATEGORY }
                        .map { (key, value) ->
                            OverviewDashboardPieSlice(
                                id = key.sliceId,
                                label = key.label,
                                value = value.asMoney(),
                            )
                        },
            )
        val cashOutByCategory =
            buildPieSlices(
                slices =
                    breakdownValueByKey.entries
                        .filter { it.key.breakdownType == BreakdownType.CASH_OUT_CATEGORY }
                        .map { (key, value) ->
                            OverviewDashboardPieSlice(
                                id = key.sliceId,
                                label = key.label,
                                value = value.asMoney(),
                            )
                        },
            )

        val cards =
            listOf(
                OverviewDashboardCard(
                    key = OverviewDashboardCardKey.BALANCE,
                    value = balanceTotal,
                    details = convertedDetailByCardKey[OverviewDashboardCardKey.BALANCE].orEmpty(),
                ),
                OverviewDashboardCard(
                    key = OverviewDashboardCardKey.PERIOD_CASH_IN,
                    value = periodCashInTotal,
                    details = convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_IN].orEmpty(),
                ),
                OverviewDashboardCard(
                    key = OverviewDashboardCardKey.PERIOD_CASH_OUT,
                    value = periodCashOutTotal,
                    details = convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_OUT].orEmpty(),
                ),
                OverviewDashboardCard(
                    key = OverviewDashboardCardKey.PERIOD_NET_CASH_FLOW,
                    value = periodNetCashFlowTotal,
                    details = periodNetCashFlowDetails,
                ),
                OverviewDashboardCard(
                    key = OverviewDashboardCardKey.PROJECTED_CASH_IN,
                    value = projectedCashInTotal,
                    details = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_IN].orEmpty(),
                ),
                OverviewDashboardCard(
                    key = OverviewDashboardCardKey.PROJECTED_CASH_OUT,
                    value = projectedCashOutTotal,
                    details = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_OUT].orEmpty(),
                ),
                OverviewDashboardCard(
                    key = OverviewDashboardCardKey.END_OF_PERIOD_BALANCE,
                    value = endOfPeriodBalanceTotal,
                    details = endOfPeriodBalanceDetails,
                ),
                OverviewDashboardCard(
                    key = OverviewDashboardCardKey.END_OF_PERIOD_NET_CASH_FLOW,
                    value = endOfPeriodNetCashFlowTotal,
                    details = endOfPeriodNetCashFlowDetails,
                ),
            )

        val charts =
            OverviewDashboardCharts(
                balance =
                    chartMonths.map { month ->
                        OverviewDashboardChartPoint(
                            month = month,
                            value = chartValueBySeriesAndMonth.getOrDefault(ChartSeries.BALANCE to month, BigDecimal.ZERO).asMoney(),
                        )
                    },
                cashIn =
                    chartMonths.map { month ->
                        OverviewDashboardChartPoint(
                            month = month,
                            value = chartValueBySeriesAndMonth.getOrDefault(ChartSeries.CASH_IN to month, BigDecimal.ZERO).asMoney(),
                        )
                    },
                cashOut =
                    chartMonths.map { month ->
                        OverviewDashboardChartPoint(
                            month = month,
                            value = chartValueBySeriesAndMonth.getOrDefault(ChartSeries.CASH_OUT to month, BigDecimal.ZERO).asMoney(),
                        )
                    },
                expense =
                    chartMonths.map { month ->
                        OverviewDashboardChartPoint(
                            month = month,
                            value = chartValueBySeriesAndMonth.getOrDefault(ChartSeries.EXPENSE to month, BigDecimal.ZERO).asMoney(),
                        )
                    },
                cashInByCategory = cashInByCategory,
                cashOutByCategory = cashOutByCategory,
                expenseByGroup = expenseByGroup,
                expenseByCategory = expenseByCategory,
            )

        return OverviewDashboard(
            selectedMonth = selectedMonth,
            currency = normalizedTargetCurrency,
            cards = cards,
            charts = charts,
        )
    }

    private suspend fun fetchExecutedByMonthByBank(
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

    private suspend fun fetchExecutedExpenseByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ) = if (minimumDate.isAfter(maximumDate)) {
        emptyList()
    } else {
        walletEntryRepository
            .summarizeOverviewExpenseByMonth(
                userId = userId,
                minimumDate = minimumDate,
                maximumDate = maximumDate,
            ).collectList()
            .awaitSingle()
    }

    private suspend fun fetchProjectedByMonthByBank(
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
                groupId = null,
                walletItemId = null,
                billDate = null,
            ).forEach { simulated ->
                val month = YearMonth.from(simulated.date)
                val byWallet = map.getOrPut(month) { mutableMapOf() }
                val isInternalBankTransfer =
                    simulated.type == com.ynixt.sharedfinances.domain.enums.WalletEntryType.TRANSFER &&
                        simulated.entries.size == 2 &&
                        simulated.entries.all { entry ->
                            entry.walletItem.type == com.ynixt.sharedfinances.domain.enums.WalletItemType.BANK_ACCOUNT &&
                                entry.walletItem.userId == userId
                        }

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

    private suspend fun fetchProjectedCreditCardCashOut(
        userId: UUID,
        selectedMonth: YearMonth,
        creditCards: List<CreditCard>,
    ): List<ProjectedCreditCardExpense> {
        if (creditCards.isEmpty()) {
            return emptyList()
        }

        val visibleCardsById = creditCards.mapNotNull { card -> card.id?.let { it to card } }.toMap()
        val openBills =
            creditCardBillService.findAllOpenByDueDateBetween(
                userId = userId,
                minimumDueDate = selectedMonth.atDay(1),
                maximumDueDate = selectedMonth.atEndOfMonth(),
            )

        return openBills
            .mapNotNull { bill ->
                val creditCard = visibleCardsById[bill.creditCardId] ?: return@mapNotNull null
                val projectedExpense = bill.value.negate().max(BigDecimal.ZERO)
                if (projectedExpense.compareTo(BigDecimal.ZERO) == 0) {
                    return@mapNotNull null
                }

                ProjectedCreditCardExpense(
                    creditCardId = bill.creditCardId,
                    creditCardName = creditCard.name,
                    currency = creditCard.currency,
                    projectedExpense = projectedExpense,
                )
            }
    }

    private fun calculateBalanceForMonth(
        bankAccount: BankAccount,
        month: YearMonth,
        currentMonth: YearMonth,
        executedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
        projectedByMonthByBankId: Map<YearMonth, Map<UUID, MonthlyAmount>>,
    ): BigDecimal {
        val bankId = bankAccount.id!!
        var result = bankAccount.balance

        if (month.isAfter(currentMonth)) {
            var cursor = currentMonth
            while (cursor.isBefore(month)) {
                result = result.add(projectedByMonthByBankId.getMonthAmount(cursor, bankId).net)
                cursor = cursor.plusMonths(1)
            }
            return result
        }

        var cursor = currentMonth
        while (cursor.isAfter(month)) {
            result = result.subtract(executedByMonthByBankId.getMonthAmount(cursor, bankId).net)
            cursor = cursor.minusMonths(1)
        }

        return result
    }

    private fun balanceReferenceDateForMonth(
        month: YearMonth,
        currentMonth: YearMonth,
        today: LocalDate,
    ): LocalDate =
        when {
            month.isBefore(currentMonth) -> month.atEndOfMonth()
            month == currentMonth -> today
            else -> month.atDay(1)
        }

    private suspend fun convertRawValues(
        rawValues: List<RawValue>,
        targetCurrency: String,
    ): Map<String, BigDecimal> {
        if (rawValues.isEmpty()) {
            return emptyMap()
        }

        val conversionRequestByKey = linkedMapOf<String, ConversionRequest>()

        rawValues.forEach { rawValue ->
            val fromCurrency = rawValue.currency.uppercase()
            if (fromCurrency == targetCurrency || rawValue.value.compareTo(BigDecimal.ZERO) == 0) {
                return@forEach
            }

            conversionRequestByKey[rawValue.key] =
                ConversionRequest(
                    value = rawValue.value,
                    fromCurrency = fromCurrency,
                    toCurrency = targetCurrency,
                    referenceDate = rawValue.referenceDate,
                )
        }

        val convertedByRequest =
            if (conversionRequestByKey.isEmpty()) {
                emptyMap()
            } else {
                exchangeRateService.convertBatch(conversionRequestByKey.values)
            }

        return rawValues.associate { rawValue ->
            val request = conversionRequestByKey[rawValue.key]
            val converted =
                if (request == null) {
                    rawValue.value
                } else {
                    convertedByRequest.getValue(request)
                }

            rawValue.key to converted.asMoney()
        }
    }

    private fun sumDetails(details: List<OverviewDashboardDetail>?): BigDecimal =
        details
            .orEmpty()
            .fold(BigDecimal.ZERO) { acc, detail -> acc.add(detail.value) }
            .asMoney()

    private fun buildMonthRange(
        start: YearMonth,
        end: YearMonth,
    ): List<YearMonth> {
        val result = mutableListOf<YearMonth>()
        var cursor = start

        while (!cursor.isAfter(end)) {
            result.add(cursor)
            cursor = cursor.plusMonths(1)
        }

        return result
    }

    private fun buildPieSlices(
        slices: List<OverviewDashboardPieSlice>,
        alwaysIncludeLabel: String? = null,
    ): List<OverviewDashboardPieSlice> {
        val sortedSlices =
            slices
                .filter { it.value.compareTo(BigDecimal.ZERO) > 0 }
                .sortedWith(compareByDescending<OverviewDashboardPieSlice> { it.value }.thenBy { it.label })

        if (sortedSlices.isEmpty()) {
            return emptyList()
        }

        val naturalNamed = sortedSlices.take(MAX_NAMED_BREAKDOWN_SLICES)
        val forcedSlice = alwaysIncludeLabel?.let { label -> sortedSlices.firstOrNull { it.label == label } }
        val namedSlices =
            if (forcedSlice == null || naturalNamed.any { it.sliceIdentity() == forcedSlice.sliceIdentity() }) {
                naturalNamed
            } else {
                (
                    sortedSlices.filterNot { it.sliceIdentity() == forcedSlice.sliceIdentity() }.take(MAX_NAMED_BREAKDOWN_SLICES - 1) +
                        forcedSlice
                ).sortedWith(compareByDescending<OverviewDashboardPieSlice> { it.value }.thenBy { it.label })
            }

        val namedIdentity = namedSlices.map { it.sliceIdentity() }.toSet()
        val otherSlices = sortedSlices.filterNot { namedIdentity.contains(it.sliceIdentity()) }
        val othersValue = otherSlices.fold(BigDecimal.ZERO) { acc, slice -> acc.add(slice.value) }.asMoney()

        if (othersValue.compareTo(BigDecimal.ZERO) <= 0) {
            return namedSlices
        }

        return namedSlices +
            OverviewDashboardPieSlice(
                id = null,
                label = PREDEFINED_OTHERS_LABEL,
                value = othersValue,
            )
    }

    private fun Map<YearMonth, Map<UUID, MonthlyAmount>>.getMonthAmount(
        month: YearMonth,
        walletItemId: UUID,
    ): MonthlyAmount = this[month]?.get(walletItemId) ?: MonthlyAmount.ZERO

    private fun BigDecimal.asMoney(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)

    private fun OverviewDashboardPieSlice.sliceIdentity(): Pair<UUID?, String> = id to label

    private data class MonthlyAmount(
        val net: BigDecimal,
        val cashIn: BigDecimal,
        val cashOut: BigDecimal,
    ) {
        companion object {
            val ZERO = MonthlyAmount(net = BigDecimal.ZERO, cashIn = BigDecimal.ZERO, cashOut = BigDecimal.ZERO)
        }

        operator fun plus(other: MonthlyAmount): MonthlyAmount =
            MonthlyAmount(
                net = net + other.net,
                cashIn = cashIn + other.cashIn,
                cashOut = cashOut + other.cashOut,
            )
    }

    private data class ProjectedCreditCardExpense(
        val creditCardId: UUID,
        val creditCardName: String,
        val currency: String,
        val projectedExpense: BigDecimal,
    )

    private data class RawValue(
        val key: String,
        val value: BigDecimal,
        val currency: String,
        val referenceDate: LocalDate,
    )

    private data class RawDetail(
        val sourceId: UUID?,
        val sourceType: OverviewDashboardDetailSourceType,
        val label: String,
        val value: BigDecimal,
        val currency: String,
        val referenceDate: LocalDate,
        val key: String = UUID.randomUUID().toString(),
    )

    private data class RawChartContribution(
        val chartSeries: ChartSeries,
        val month: YearMonth,
        val value: BigDecimal,
        val currency: String,
        val referenceDate: LocalDate,
        val key: String = UUID.randomUUID().toString(),
    )

    private data class RawBreakdownContribution(
        val breakdownType: BreakdownType,
        val sliceId: UUID?,
        val label: String,
        val value: BigDecimal,
        val currency: String,
        val referenceDate: LocalDate,
        val key: String = UUID.randomUUID().toString(),
    )

    private data class BreakdownSliceKey(
        val breakdownType: BreakdownType,
        val sliceId: UUID?,
        val label: String,
    )

    private enum class ChartSeries {
        BALANCE,
        CASH_IN,
        CASH_OUT,
        EXPENSE,
    }

    private enum class BreakdownType {
        CASH_IN_CATEGORY,
        CASH_OUT_CATEGORY,
        EXPENSE_GROUP,
        EXPENSE_CATEGORY,
    }

    companion object {
        private const val MAX_NAMED_BREAKDOWN_SLICES = 9
        private const val PREDEFINED_INDIVIDUAL_LABEL = "PREDEFINED_INDIVIDUAL"
        private const val PREDEFINED_UNCATEGORIZED_LABEL = "PREDEFINED_UNCATEGORIZED"
        private const val PREDEFINED_OTHERS_LABEL = "PREDEFINED_OTHERS"
    }
}
