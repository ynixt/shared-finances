package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.impl.BankAccountMapperImpl
import com.ynixt.sharedfinances.domain.mapper.impl.CreditCardMapperImpl
import com.ynixt.sharedfinances.domain.mapper.impl.WalletItemMapperImpl
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.dashboard.BankAccountMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashDirection
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseMonthlySummary
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEntryRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletItemRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID
import kotlin.system.measureTimeMillis

class OverviewDashboardServiceImplTest {
    @Test
    fun `should calculate formulas for selected month`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val bankId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            walletItemRepository.save(
                bankAccountEntity(
                    id = bankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                ),
            )

            val walletEntryRepository =
                stubWalletEntryRepository(
                    monthlySummaries =
                        listOf(
                            monthly(bankId, "2026-04", net = "200.00", cashIn = "300.00", cashOut = "100.00"),
                        ),
                )

            val recurrenceSimulationService =
                fakeRecurrenceSimulationService(
                    events =
                        listOf(
                            simulatedEvent(
                                date = LocalDate.of(2026, 4, 20),
                                walletItemId = bankId,
                                value = "100.00",
                                walletItem =
                                    walletFromEntity(
                                        bankAccountEntity(
                                            id = bankId,
                                            userId = userId,
                                            name = "Main",
                                            currency = "BRL",
                                            balance = "1000.00",
                                            showOnDashboard = true,
                                        ),
                                    ),
                            ),
                            simulatedEvent(
                                date = LocalDate.of(2026, 4, 22),
                                walletItemId = bankId,
                                value = "-50.00",
                                walletItem =
                                    walletFromEntity(
                                        bankAccountEntity(
                                            id = bankId,
                                            userId = userId,
                                            name = "Main",
                                            currency = "BRL",
                                            balance = "1000.00",
                                            showOnDashboard = true,
                                        ),
                                    ),
                            ),
                        ),
                )

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = recurrenceSimulationService,
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cardValue(OverviewDashboardCardKey.BALANCE), "1000.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.PERIOD_CASH_IN), "300.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.PERIOD_CASH_OUT), "100.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.PERIOD_NET_CASH_FLOW), "200.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_IN), "100.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_OUT), "50.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.END_OF_PERIOD_BALANCE), "1050.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.END_OF_PERIOD_NET_CASH_FLOW), "50.00")
            assertMoney(result.balanceValue("2026-04"), "1000.00")
        }

    @Test
    fun `should use future opening balance rule for saldo`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 6)
            val bankId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            walletItemRepository.save(
                bankAccountEntity(
                    id = bankId,
                    userId = userId,
                    name = "Future account",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                ),
            )

            val recurrenceSimulationService =
                fakeRecurrenceSimulationService(
                    events =
                        listOf(
                            simulatedEvent(
                                date = LocalDate.of(2026, 4, 20),
                                walletItemId = bankId,
                                value = "200.00",
                                walletItem =
                                    walletFromEntity(
                                        bankAccountEntity(
                                            id = bankId,
                                            userId = userId,
                                            name = "Future account",
                                            currency = "BRL",
                                            balance = "1000.00",
                                            showOnDashboard = true,
                                        ),
                                    ),
                            ),
                            simulatedEvent(
                                date = LocalDate.of(2026, 5, 10),
                                walletItemId = bankId,
                                value = "300.00",
                                walletItem =
                                    walletFromEntity(
                                        bankAccountEntity(
                                            id = bankId,
                                            userId = userId,
                                            name = "Future account",
                                            currency = "BRL",
                                            balance = "1000.00",
                                            showOnDashboard = true,
                                        ),
                                    ),
                            ),
                        ),
                )

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = stubWalletEntryRepository(emptyList()),
                    recurrenceSimulationService = recurrenceSimulationService,
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cardValue(OverviewDashboardCardKey.BALANCE), "1500.00")
        }

    @Test
    fun `should include projected unpaid credit card bills and exclude hidden items`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val visibleBankId = UUID.randomUUID()
            val hiddenBankId = UUID.randomUUID()
            val visibleCardId = UUID.randomUUID()
            val hiddenCardId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            walletItemRepository.save(
                bankAccountEntity(
                    id = visibleBankId,
                    userId = userId,
                    name = "Visible bank",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                ),
            )
            walletItemRepository.save(
                bankAccountEntity(
                    id = hiddenBankId,
                    userId = userId,
                    name = "Hidden bank",
                    currency = "BRL",
                    balance = "9999.00",
                    showOnDashboard = false,
                ),
            )
            walletItemRepository.save(
                creditCardEntity(
                    id = visibleCardId,
                    userId = userId,
                    name = "Visible card",
                    currency = "BRL",
                    totalLimit = "1000.00",
                    availableLimit = "800.00",
                    showOnDashboard = true,
                ),
            )
            walletItemRepository.save(
                creditCardEntity(
                    id = hiddenCardId,
                    userId = userId,
                    name = "Hidden card",
                    currency = "BRL",
                    totalLimit = "1000.00",
                    availableLimit = "800.00",
                    showOnDashboard = false,
                ),
            )

            val walletEntryRepository =
                stubWalletEntryRepository(
                    monthlySummaries =
                        listOf(
                            monthly(visibleBankId, "2026-04", net = "0.00", cashIn = "0.00", cashOut = "0.00"),
                            monthly(hiddenBankId, "2026-04", net = "0.00", cashIn = "0.00", cashOut = "0.00"),
                        ),
                )

            val recurrenceSimulationService =
                fakeRecurrenceSimulationService(
                    events =
                        listOf(
                            simulatedEvent(
                                date = LocalDate.of(2026, 4, 20),
                                walletItemId = visibleBankId,
                                value = "-100.00",
                                walletItem =
                                    walletFromEntity(
                                        bankAccountEntity(
                                            id = visibleBankId,
                                            userId = userId,
                                            name = "Visible bank",
                                            currency = "BRL",
                                            balance = "1000.00",
                                            showOnDashboard = true,
                                        ),
                                    ),
                            ),
                            simulatedEvent(
                                date = LocalDate.of(2026, 4, 21),
                                walletItemId = hiddenBankId,
                                value = "-500.00",
                                walletItem =
                                    walletFromEntity(
                                        bankAccountEntity(
                                            id = hiddenBankId,
                                            userId = userId,
                                            name = "Hidden bank",
                                            currency = "BRL",
                                            balance = "9999.00",
                                            showOnDashboard = false,
                                        ),
                                    ),
                            ),
                        ),
                )

            val billByCreditCardId =
                mapOf(
                    visibleCardId to bill(visibleCardId, value = "-400.00", paid = false),
                    hiddenCardId to bill(hiddenCardId, value = "-700.00", paid = false),
                )

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = recurrenceSimulationService,
                    creditCardBillService = fakeCreditCardBillService(billByCreditCardId),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_OUT), "500.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.END_OF_PERIOD_NET_CASH_FLOW), "-500.00")
            assertThat(
                result.cards
                    .first { it.key == OverviewDashboardCardKey.PROJECTED_CASH_OUT }
                    .details
                    .map { it.label },
            ).contains("Visible bank", "Visible card")
                .doesNotContain("Hidden bank", "Hidden card")
        }

    @Test
    fun `should keep expense chart separate from cash out totals`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val bankId = UUID.randomUUID()
            val cardId = UUID.randomUUID()
            val walletItemRepository = InMemoryWalletItemRepository()

            walletItemRepository.save(
                bankAccountEntity(
                    id = bankId,
                    userId = userId,
                    name = "Main bank",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                ),
            )
            walletItemRepository.save(
                creditCardEntity(
                    id = cardId,
                    userId = userId,
                    name = "Main card",
                    currency = "BRL",
                    totalLimit = "1000.00",
                    availableLimit = "800.00",
                    showOnDashboard = true,
                ),
            )

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository =
                        stubWalletEntryRepository(
                            monthlySummaries =
                                listOf(
                                    monthly(
                                        walletItemId = bankId,
                                        month = "2026-04",
                                        net = "-50.00",
                                        cashIn = "0.00",
                                        cashOut = "50.00",
                                    ),
                                ),
                            expenseMonthlySummaries = listOf(expenseByMonth(month = "2026-04", expense = "80.00")),
                        ),
                    recurrenceSimulationService = fakeRecurrenceSimulationService(emptyList()),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cardValue(OverviewDashboardCardKey.PERIOD_CASH_OUT), "50.00")
            assertMoney(result.expenseValue("2026-04"), "80.00")
        }

    @Test
    fun `should exclude projected internal bank transfers from cash totals`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val originBankId = UUID.randomUUID()
            val targetBankId = UUID.randomUUID()
            val originBank = bankAccountEntity(originBankId, userId, "Origin", "BRL", "1000.00", true)
            val targetBank = bankAccountEntity(targetBankId, userId, "Target", "BRL", "900.00", true)
            val walletItemRepository = InMemoryWalletItemRepository()
            walletItemRepository.save(originBank)
            walletItemRepository.save(targetBank)

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = stubWalletEntryRepository(emptyList()),
                    recurrenceSimulationService =
                        fakeRecurrenceSimulationService(
                            listOf(
                                simulatedTransferEvent(
                                    date = LocalDate.of(2026, 4, 20),
                                    entries =
                                        listOf(
                                            simulatedEntry(originBankId, walletFromEntity(originBank), "-100.00"),
                                            simulatedEntry(targetBankId, walletFromEntity(targetBank), "100.00"),
                                        ),
                                ),
                            ),
                        ),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_IN), "0.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_OUT), "0.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.END_OF_PERIOD_NET_CASH_FLOW), "0.00")
        }

    @Test
    fun `should keep overview execution under target on synthetic workload`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val walletItemRepository = InMemoryWalletItemRepository()
            val bankIds = mutableListOf<UUID>()

            repeat(50) { idx ->
                val bankId = UUID.randomUUID()
                bankIds.add(bankId)
                walletItemRepository.save(
                    bankAccountEntity(
                        id = bankId,
                        userId = userId,
                        name = "Bank-$idx",
                        currency = "BRL",
                        balance = "10000.00",
                        showOnDashboard = true,
                    ),
                )
            }

            val monthlySummaries = mutableListOf<BankAccountMonthlySummary>()
            val monthStart = selectedMonth.minusMonths(23)
            repeat(24) { monthOffset ->
                val month = monthStart.plusMonths(monthOffset.toLong())
                bankIds.forEach { bankId ->
                    monthlySummaries.add(
                        BankAccountMonthlySummary(
                            walletItemId = bankId,
                            month = month,
                            net = BigDecimal("100.00"),
                            cashIn = BigDecimal("150.00"),
                            cashOut = BigDecimal("50.00"),
                        ),
                    )
                }
            }

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = stubWalletEntryRepository(monthlySummaries),
                    recurrenceSimulationService = fakeRecurrenceSimulationService(emptyList()),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                )

            val measurements =
                (1..20)
                    .map {
                        var duration = 0L
                        duration =
                            measureTimeMillis {
                                runBlocking {
                                    service.getOverview(
                                        userId = userId,
                                        defaultCurrency = "BRL",
                                        selectedMonth = selectedMonth,
                                    )
                                }
                            }
                        duration
                    }.sorted()

            val p95 = measurements[(measurements.size * 95 / 100).coerceAtMost(measurements.size - 1)]
            assertThat(p95).isLessThanOrEqualTo(400L)
        }

    @Test
    fun `should build selected-month expense breakdowns with fallbacks top 9 and others`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val bankId = UUID.randomUUID()
            val walletItemRepository = InMemoryWalletItemRepository()

            walletItemRepository.save(
                bankAccountEntity(
                    id = bankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                ),
            )

            val walletEntryRepository =
                stubWalletEntryRepository(
                    monthlySummaries =
                        listOf(
                            monthly(
                                walletItemId = bankId,
                                month = "2026-04",
                                net = "-160.00",
                                cashIn = "0.00",
                                cashOut = "160.00",
                            ),
                        ),
                    expenseMonthlySummaries = listOf(expenseByMonth(month = "2026-04", expense = "160.00")),
                    expenseBreakdowns =
                        listOf(
                            breakdown(groupName = "Group 1", categoryName = "Category 1", expense = "20.00"),
                            breakdown(groupName = "Group 2", categoryName = "Category 2", expense = "19.00"),
                            breakdown(groupName = "Group 3", categoryName = "Category 3", expense = "18.00"),
                            breakdown(groupName = "Group 4", categoryName = "Category 4", expense = "17.00"),
                            breakdown(groupName = "Group 5", categoryName = "Category 5", expense = "16.00"),
                            breakdown(groupName = "Group 6", categoryName = "Category 6", expense = "15.00"),
                            breakdown(groupName = "Group 7", categoryName = "Category 7", expense = "14.00"),
                            breakdown(groupName = "Group 8", categoryName = "Category 8", expense = "13.00"),
                            breakdown(groupName = "Group 9", categoryName = "Category 9", expense = "12.00"),
                            breakdown(groupName = "Group 10", categoryName = "Category 10", expense = "11.00"),
                            breakdown(groupName = null, categoryName = null, expense = "5.00"),
                        ),
                )

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = fakeRecurrenceSimulationService(emptyList()),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.expenseValue("2026-04"), "160.00")
            assertThat(result.charts.expenseByGroup.map { it.label })
                .contains("PREDEFINED_INDIVIDUAL", "PREDEFINED_OTHERS")
                .doesNotContain("Group 9", "Group 10")
            assertMoney(result.groupSliceValue("PREDEFINED_INDIVIDUAL"), "5.00")
            assertMoney(result.groupSliceValue("PREDEFINED_OTHERS"), "23.00")

            assertThat(result.charts.expenseByCategory.map { it.label })
                .contains("PREDEFINED_UNCATEGORIZED", "PREDEFINED_OTHERS")
                .doesNotContain("Category 9", "Category 10")
            assertMoney(result.categorySliceValue("PREDEFINED_UNCATEGORIZED"), "5.00")
            assertMoney(result.categorySliceValue("PREDEFINED_OTHERS"), "23.00")
        }

    @Test
    fun `should use only user attributable amount in shared group breakdown`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val bankId = UUID.randomUUID()
            val walletItemRepository = InMemoryWalletItemRepository()

            walletItemRepository.save(
                bankAccountEntity(
                    id = bankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                ),
            )

            val walletEntryRepository =
                stubWalletEntryRepository(
                    monthlySummaries =
                        listOf(
                            monthly(
                                walletItemId = bankId,
                                month = "2026-04",
                                net = "-80.00",
                                cashIn = "0.00",
                                cashOut = "80.00",
                            ),
                        ),
                    expenseMonthlySummaries = listOf(expenseByMonth(month = "2026-04", expense = "80.00")),
                    expenseBreakdowns =
                        listOf(
                            breakdown(groupName = "Trip", categoryName = "Travel", expense = "80.00"),
                        ),
                )

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = fakeRecurrenceSimulationService(emptyList()),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.expenseValue("2026-04"), "80.00")
            assertMoney(result.groupSliceValue("Trip"), "80.00")
            assertThat(result.charts.expenseByGroup.map { it.label }).doesNotContain("PREDEFINED_OTHERS")
        }

    private fun createService(
        clock: Clock,
        walletItemRepository: InMemoryWalletItemRepository,
        walletEntryRepository: WalletEntryRepository,
        recurrenceSimulationService: RecurrenceSimulationService,
        creditCardBillService: CreditCardBillService,
        exchangeRateService: ExchangeRateService,
    ): OverviewDashboardServiceImpl =
        OverviewDashboardServiceImpl(
            walletItemRepository = walletItemRepository,
            walletItemMapper = WalletItemMapperImpl(BankAccountMapperImpl(), CreditCardMapperImpl()),
            walletEntryRepository = walletEntryRepository,
            recurrenceSimulationService = recurrenceSimulationService,
            creditCardBillService = creditCardBillService,
            exchangeRateService = exchangeRateService,
            clock = clock,
        )

    private fun stubWalletEntryRepository(
        monthlySummaries: List<BankAccountMonthlySummary>,
        expenseMonthlySummaries: List<OverviewExpenseMonthlySummary> = emptyList(),
        cashBreakdowns: List<OverviewCashBreakdownSummary> = emptyList(),
        expenseBreakdowns: List<OverviewExpenseBreakdownSummary> = emptyList(),
    ): WalletEntryRepository {
        val delegate = InMemoryWalletEntryRepository()

        return object : WalletEntryRepository by delegate {
            override fun summarizeBankAccountsByMonth(
                userId: UUID,
                minimumDate: LocalDate,
                maximumDate: LocalDate,
            ): Flux<BankAccountMonthlySummary> =
                Flux.fromIterable(
                    monthlySummaries.filter {
                        val date = it.month.atDay(1)
                        !date.isBefore(minimumDate) && !date.isAfter(maximumDate)
                    },
                )

            override fun sumForBankAccountSummary(
                userId: UUID?,
                groupId: UUID?,
                walletItemId: UUID?,
                minimumDate: LocalDate,
                maximumDate: LocalDate?,
            ): Flux<EntrySumResult> = Flux.empty()

            override fun summarizeOverviewExpenseByMonth(
                userId: UUID,
                minimumDate: LocalDate,
                maximumDate: LocalDate,
            ): Flux<OverviewExpenseMonthlySummary> =
                Flux.fromIterable(
                    expenseMonthlySummaries.filter {
                        val date = it.month.atDay(1)
                        !date.isBefore(minimumDate) && !date.isAfter(maximumDate)
                    },
                )

            override fun summarizeOverviewCashBreakdown(
                userId: UUID,
                minimumDate: LocalDate,
                maximumDate: LocalDate,
            ): Flux<OverviewCashBreakdownSummary> = Flux.fromIterable(cashBreakdowns)

            override fun summarizeOverviewExpenseBreakdown(
                userId: UUID,
                minimumDate: LocalDate,
                maximumDate: LocalDate,
            ): Flux<OverviewExpenseBreakdownSummary> = Flux.fromIterable(expenseBreakdowns)
        }
    }

    private fun fakeRecurrenceSimulationService(events: List<EventListResponse>): RecurrenceSimulationService =
        object : RecurrenceSimulationService {
            override suspend fun getFutureValuesOfWalletItem(
                walletItemId: UUID,
                minimumEndExecution: LocalDate,
                maximumNextExecution: LocalDate,
                userId: UUID,
                groupId: UUID?,
            ): BigDecimal = BigDecimal.ZERO

            override suspend fun getFutureValuesOCreditCard(
                bill: CreditCardBill,
                userId: UUID,
                groupId: UUID?,
                walletItemId: UUID,
            ): BigDecimal = BigDecimal.ZERO

            override suspend fun simulateGeneration(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userId: UUID?,
                groupId: UUID?,
                walletItemId: UUID?,
                billDate: LocalDate?,
            ): List<EventListResponse> =
                events.filter {
                    (minimumEndExecution == null || !it.date.isBefore(minimumEndExecution)) &&
                        (maximumNextExecution == null || !it.date.isAfter(maximumNextExecution))
                }

            override suspend fun simulateGenerationAsEntrySumResult(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userId: UUID?,
                groupId: UUID?,
                walletItemId: UUID?,
            ): List<EntrySumResult> = emptyList()

            override suspend fun simulateGenerationForCreditCard(
                billDate: LocalDate,
                userId: UUID,
                groupId: UUID?,
                walletItemId: UUID,
            ): List<EventListResponse> = emptyList()

            override suspend fun simulateGenerationForCreditCard(
                bill: CreditCardBill,
                userId: UUID,
                groupId: UUID?,
                walletItemId: UUID?,
            ): List<EventListResponse> = emptyList()

            override suspend fun simulateGeneration(
                config: com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity,
                walletItems: List<WalletItem>,
                user: com.ynixt.sharedfinances.domain.entities.UserEntity?,
                group: com.ynixt.sharedfinances.domain.entities.groups.GroupEntity?,
                category: com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity?,
                simulateBillForRecurrence: Boolean,
            ): EventListResponse = events.first()
        }

    private fun fakeCreditCardBillService(billsByCreditCardId: Map<UUID, CreditCardBill>): CreditCardBillService =
        object : CreditCardBillService {
            override suspend fun getOrCreateBill(
                creditCardId: UUID,
                dueDate: LocalDate,
                closingDate: LocalDate,
                startValue: BigDecimal,
            ): com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity = error("not used")

            override suspend fun changeClosingDate(
                userId: UUID,
                creditCardId: UUID,
                closingDate: LocalDate,
            ) = Unit

            override suspend fun changeDueDate(
                userId: UUID,
                creditCardId: UUID,
                dueDate: LocalDate,
            ) = Unit

            override suspend fun addValueById(
                id: UUID,
                value: BigDecimal,
            ): Long = 0

            override suspend fun findById(id: UUID): CreditCardBill? = billsByCreditCardId[id]

            override suspend fun findAuthorizedById(
                userId: UUID,
                id: UUID,
            ): CreditCardBill? = billsByCreditCardId.values.firstOrNull { it.id == id }

            override suspend fun findAllOpenByDueDateBetween(
                userId: UUID,
                minimumDueDate: LocalDate,
                maximumDueDate: LocalDate,
            ): List<CreditCardBill> =
                billsByCreditCardId.values.filter {
                    !it.dueDate.isBefore(minimumDueDate) &&
                        !it.dueDate.isAfter(maximumDueDate) &&
                        it.value < BigDecimal.ZERO
                }

            override suspend fun getBillFromDatabaseOrSimulate(
                userId: UUID,
                creditCardId: UUID,
                billDate: LocalDate,
            ): CreditCardBill =
                billsByCreditCardId.getOrElse(creditCardId) {
                    bill(creditCardId, value = "0.00", paid = false)
                }
        }

    private fun identityExchangeRateService(): ExchangeRateService =
        object : ExchangeRateService {
            override suspend fun syncLatestQuotes(): Int = 0

            override suspend fun syncQuotesForDate(
                date: LocalDate,
                baseCurrencies: Set<String>?,
            ): Int = 0

            override suspend fun listQuotes(
                request: ExchangeRateQuoteListRequest,
            ): CursorPage<com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity> =
                CursorPage(
                    items = emptyList(),
                    nextCursor = null,
                    hasNext = false,
                )

            override suspend fun getRate(
                fromCurrency: String,
                toCurrency: String,
                referenceDate: LocalDate,
            ): BigDecimal = BigDecimal.ONE

            override suspend fun resolveRate(
                fromCurrency: String,
                toCurrency: String,
                referenceDate: LocalDate,
            ): ResolvedExchangeRate = ResolvedExchangeRate(rate = BigDecimal.ONE, quoteDate = referenceDate)

            override suspend fun convert(
                value: BigDecimal,
                fromCurrency: String,
                toCurrency: String,
                referenceDate: LocalDate,
            ): BigDecimal = value

            override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> =
                requests.associateWith { it.value }
        }

    private fun fixedClock(instant: String): Clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)

    private fun bankAccountEntity(
        id: UUID,
        userId: UUID,
        name: String,
        currency: String,
        balance: String,
        showOnDashboard: Boolean,
    ): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.BANK_ACCOUNT,
            name = name,
            enabled = true,
            userId = userId,
            currency = currency,
            balance = BigDecimal(balance),
            totalLimit = null,
            dueDay = null,
            daysBetweenDueAndClosing = null,
            dueOnNextBusinessDay = null,
            showOnDashboard = showOnDashboard,
        ).also { it.id = id }

    private fun creditCardEntity(
        id: UUID,
        userId: UUID,
        name: String,
        currency: String,
        totalLimit: String,
        availableLimit: String,
        showOnDashboard: Boolean,
    ): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.CREDIT_CARD,
            name = name,
            enabled = true,
            userId = userId,
            currency = currency,
            balance = BigDecimal(availableLimit),
            totalLimit = BigDecimal(totalLimit),
            dueDay = 10,
            daysBetweenDueAndClosing = 7,
            dueOnNextBusinessDay = true,
            showOnDashboard = showOnDashboard,
        ).also { it.id = id }

    private fun walletFromEntity(entity: WalletItemEntity): WalletItem =
        WalletItemMapperImpl(BankAccountMapperImpl(), CreditCardMapperImpl()).toModel(entity)

    private fun monthly(
        walletItemId: UUID,
        month: String,
        net: String,
        cashIn: String,
        cashOut: String,
    ): BankAccountMonthlySummary =
        BankAccountMonthlySummary(
            walletItemId = walletItemId,
            month = YearMonth.parse(month),
            net = BigDecimal(net),
            cashIn = BigDecimal(cashIn),
            cashOut = BigDecimal(cashOut),
        )

    private fun expenseByMonth(
        month: String,
        expense: String,
        currency: String = "BRL",
    ): OverviewExpenseMonthlySummary =
        OverviewExpenseMonthlySummary(
            month = YearMonth.parse(month),
            currency = currency,
            expense = BigDecimal(expense),
        )

    private fun cashBreakdown(
        direction: OverviewCashDirection,
        categoryName: String?,
        amount: String,
        currency: String = "BRL",
    ): OverviewCashBreakdownSummary =
        OverviewCashBreakdownSummary(
            direction = direction,
            categoryId = if (categoryName == null) null else UUID.nameUUIDFromBytes(categoryName.toByteArray()),
            categoryName = categoryName,
            currency = currency,
            amount = BigDecimal(amount),
        )

    private fun breakdown(
        groupName: String?,
        categoryName: String?,
        expense: String,
    ): OverviewExpenseBreakdownSummary =
        OverviewExpenseBreakdownSummary(
            groupId = if (groupName == null) null else UUID.nameUUIDFromBytes(groupName.toByteArray()),
            groupName = groupName,
            categoryId = if (categoryName == null) null else UUID.nameUUIDFromBytes(categoryName.toByteArray()),
            categoryName = categoryName,
            currency = "BRL",
            expense = BigDecimal(expense),
        )

    private fun simulatedEvent(
        date: LocalDate,
        walletItemId: UUID,
        walletItem: WalletItem,
        value: String,
    ): EventListResponse =
        EventListResponse(
            id = UUID.randomUUID(),
            type = com.ynixt.sharedfinances.domain.enums.WalletEntryType.REVENUE,
            name = "simulated",
            category = null,
            user = null,
            group = null,
            tags = emptyList(),
            observations = null,
            date = date,
            confirmed = false,
            installment = null,
            recurrenceConfigId = null,
            recurrenceConfig = null,
            currency = walletItem.currency,
            entries = listOf(simulatedEntry(walletItemId = walletItemId, walletItem = walletItem, value = value)),
        )

    private fun simulatedTransferEvent(
        date: LocalDate,
        entries: List<EventListResponse.EntryResponse>,
    ): EventListResponse =
        EventListResponse(
            id = UUID.randomUUID(),
            type = com.ynixt.sharedfinances.domain.enums.WalletEntryType.TRANSFER,
            name = "simulated-transfer",
            category = null,
            user = null,
            group = null,
            tags = emptyList(),
            observations = null,
            date = date,
            confirmed = false,
            installment = null,
            recurrenceConfigId = null,
            recurrenceConfig = null,
            currency = entries.first().walletItem.currency,
            entries = entries,
        )

    private fun simulatedEntry(
        walletItemId: UUID,
        walletItem: WalletItem,
        value: String,
    ): EventListResponse.EntryResponse =
        EventListResponse.EntryResponse(
            value = BigDecimal(value),
            walletItem = walletItem,
            walletItemId = walletItemId,
            billDate = null,
            billId = null,
        )

    private fun bill(
        creditCardId: UUID,
        value: String,
        paid: Boolean,
    ): CreditCardBill =
        CreditCardBill(
            id = UUID.randomUUID(),
            creditCardId = creditCardId,
            billDate = LocalDate.of(2026, 4, 1),
            dueDate = LocalDate.of(2026, 4, 10),
            closingDate = LocalDate.of(2026, 4, 3),
            paid = paid,
            value = BigDecimal(value),
        )

    private fun assertMoney(
        current: BigDecimal,
        expected: String,
    ) {
        assertThat(current).isEqualByComparingTo(BigDecimal(expected))
    }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.cardValue(key: OverviewDashboardCardKey): BigDecimal =
        cards.first { it.key == key }.value

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.balanceValue(month: String): BigDecimal =
        charts.balance.first { it.month == YearMonth.parse(month) }.value

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.groupSliceValue(label: String): BigDecimal =
        charts.expenseByGroup.first { it.label == label }.value

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.categorySliceValue(label: String): BigDecimal =
        charts.expenseByCategory.first { it.label == label }.value

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.expenseValue(month: String): BigDecimal =
        charts.expense.first { it.month == YearMonth.parse(month) }.value
}
