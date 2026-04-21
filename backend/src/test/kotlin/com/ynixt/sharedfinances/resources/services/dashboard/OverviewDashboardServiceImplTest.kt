package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.impl.BankAccountMapperImpl
import com.ynixt.sharedfinances.domain.mapper.impl.CreditCardMapperImpl
import com.ynixt.sharedfinances.domain.mapper.impl.WalletItemMapperImpl
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.dashboard.BankAccountMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashDirection
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExecutedBankFactSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExecutedExpenseFactSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseSourceSummary
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyCashFlow
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByGoalRow
import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByWalletRow
import com.ynixt.sharedfinances.domain.repositories.GoalCurrencyCommittedRow
import com.ynixt.sharedfinances.domain.repositories.GoalLedgerCommittedSummaryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.scenarios.support.NoOpGoalLedgerCommittedSummaryRepository
import com.ynixt.sharedfinances.scenarios.support.NoOpGroupDebtService
import com.ynixt.sharedfinances.scenarios.support.NoOpGroupService
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
    fun `group overview should include categorized and uncategorized expenses`() =
        runBlocking {
            val actorUserId = UUID.randomUUID()
            val memberUserId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val actorBankId = UUID.randomUUID()
            val memberBankId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val actorBank =
                bankAccountEntity(
                    id = actorBankId,
                    userId = actorUserId,
                    name = "Actor bank",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                )
            val memberBank =
                bankAccountEntity(
                    id = memberBankId,
                    userId = memberUserId,
                    name = "Member bank",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                )
            walletItemRepository.save(actorBank)
            walletItemRepository.save(memberBank)

            val actorWalletItem = walletFromEntity(actorBank)
            val memberWalletItem = walletFromEntity(memberBank)
            val categorizedExpense =
                EventListResponse(
                    id = UUID.randomUUID(),
                    type = WalletEntryType.EXPENSE,
                    name = "categorized expense",
                    category = categoryEntity("Lazer"),
                    user = null,
                    group = null,
                    tags = emptyList(),
                    observations = null,
                    date = LocalDate.of(2026, 4, 21),
                    confirmed = false,
                    installment = null,
                    recurrenceConfigId = null,
                    recurrenceConfig = null,
                    currency = "BRL",
                    entries = listOf(simulatedEntry(walletItemId = memberBankId, walletItem = memberWalletItem, value = "-3.00")),
                )
            val uncategorizedExpense =
                EventListResponse(
                    id = UUID.randomUUID(),
                    type = WalletEntryType.EXPENSE,
                    name = "uncategorized expense",
                    category = null,
                    user = null,
                    group = null,
                    tags = emptyList(),
                    observations = null,
                    date = LocalDate.of(2026, 4, 21),
                    confirmed = false,
                    installment = null,
                    recurrenceConfigId = null,
                    recurrenceConfig = null,
                    currency = "BRL",
                    entries = listOf(simulatedEntry(walletItemId = actorBankId, walletItem = actorWalletItem, value = "-500.00")),
                )

            val walletEventListService =
                object : WalletEventListService by fakeWalletEventListService() {
                    override suspend fun list(
                        userId: UUID,
                        request: ListEntryRequest,
                    ): CursorPage<EventListResponse> {
                        val all = listOf(categorizedExpense, uncategorizedExpense)
                        val filtered =
                            if (request.includeUncategorized && request.categoryIds.isEmpty()) {
                                all.filter { it.category == null }
                            } else {
                                all
                            }

                        return CursorPage(items = filtered, nextCursor = null, hasNext = false)
                    }
                }

            val groupService =
                object : GroupService by NoOpGroupService() {
                    override suspend fun findAllMembers(
                        userId: UUID,
                        id: UUID,
                    ): List<GroupUserEntity> =
                        if (id == groupId) {
                            listOf(
                                GroupUserEntity(groupId = groupId, userId = actorUserId, role = UserGroupRole.EDITOR),
                                GroupUserEntity(groupId = groupId, userId = memberUserId, role = UserGroupRole.EDITOR),
                            )
                        } else {
                            emptyList()
                        }
                }

            val service =
                createService(
                    clock = fixedClock("2026-04-21T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = stubWalletEntryRepository(emptyList()),
                    recurrenceSimulationService = fakeRecurrenceSimulationService(emptyList()),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                    groupService = groupService,
                    walletEventListService = walletEventListService,
                )

            val result =
                service.getGroupOverview(
                    userId = actorUserId,
                    groupId = groupId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cards.first { it.key == OverviewDashboardCardKey.PERIOD_EXPENSES }.value, "503.00")
            assertMoney(
                result.charts.expense.total.first { it.month == selectedMonth }.executedValue,
                "503.00",
            )
            assertMoney(
                result.charts.expense.byMember
                    .first { it.memberId == memberUserId }
                    .points
                    .first { it.month == selectedMonth }
                    .executedValue,
                "3.00",
            )
            assertMoney(
                result.charts.expenseByCategory.fold(BigDecimal.ZERO) { acc, slice -> acc.add(slice.value) },
                "503.00",
            )
        }

    @Test
    fun `should include goal committed and free balance from ledger summary`() =
        runBlocking {
            val ownerUserId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val bankId = UUID.randomUUID()
            val hiddenBankId = UUID.randomUUID()
            val goalId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            walletItemRepository.save(
                bankAccountEntity(
                    id = bankId,
                    userId = ownerUserId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                ),
            )
            walletItemRepository.save(
                bankAccountEntity(
                    id = hiddenBankId,
                    userId = ownerUserId,
                    name = "Hidden reserve",
                    currency = "BRL",
                    balance = "400.00",
                    showOnDashboard = false,
                ),
            )

            val walletEntryRepository =
                stubWalletEntryRepository(
                    monthlySummaries =
                        listOf(
                            monthly(bankId, "2026-04", net = "0", cashIn = "0", cashOut = "0"),
                        ),
                )

            val goalLedgerSummaryRepository =
                object : GoalLedgerCommittedSummaryRepository {
                    override fun summarizeCommittedByUserGoals(userId: UUID): Flux<GoalCommittedByWalletRow> = Flux.empty()

                    override fun summarizeCommittedByUserGoalsDetailed(userId: UUID) =
                        if (userId == ownerUserId) {
                            Flux.just(
                                GoalCommittedByGoalRow(
                                    goalId = goalId,
                                    goalName = "Reserve",
                                    walletItemId = bankId,
                                    currency = "BRL",
                                    committed = BigDecimal("250.00"),
                                ),
                                GoalCommittedByGoalRow(
                                    goalId = goalId,
                                    goalName = "Reserve",
                                    walletItemId = hiddenBankId,
                                    currency = "BRL",
                                    committed = BigDecimal("100.00"),
                                ),
                            )
                        } else {
                            Flux.empty()
                        }

                    override fun summarizeCommittedByGroupGoals(groupId: UUID): Flux<GoalCommittedByWalletRow> = Flux.empty()

                    override fun summarizeCommittedByGroupGoalsDetailed(groupId: UUID): Flux<GoalCommittedByGoalRow> = Flux.empty()

                    override fun summarizeCommittedByGoal(goalId: UUID): Flux<GoalCurrencyCommittedRow> = Flux.empty()
                }

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = fakeRecurrenceSimulationService(emptyList()),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                    goalLedgerSummaryRepository = goalLedgerSummaryRepository,
                )

            val result =
                service.getOverview(
                    userId = ownerUserId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.goalCommittedTotal, "250.00")
            assertMoney(result.freeBalanceTotal, "750.00")
            assertThat(result.goalOverCommittedWarning).isFalse()
            assertThat(result.cards.take(3).map { it.key }).containsExactly(
                OverviewDashboardCardKey.BALANCE,
                OverviewDashboardCardKey.GOAL_COMMITTED,
                OverviewDashboardCardKey.GOAL_FREE_BALANCE,
            )
            assertThat(
                result.cards
                    .first { it.key == OverviewDashboardCardKey.GOAL_COMMITTED }
                    .details,
            ).singleElement().satisfies({ parent ->
                assertThat(parent.sourceId).isEqualTo(bankId)
                assertThat(parent.sourceType).isEqualTo(OverviewDashboardDetailSourceType.BANK_ACCOUNT)
                assertThat(parent.label).isEqualTo("Main")
                assertMoney(parent.value, "250.00")
                assertThat(parent.accountOverCommitted).isFalse()
                assertThat(parent.children).singleElement().satisfies({ child ->
                    assertThat(child.sourceId).isEqualTo(goalId)
                    assertThat(child.sourceType).isEqualTo(OverviewDashboardDetailSourceType.GOAL)
                    assertThat(child.label).isEqualTo("Reserve")
                    assertMoney(child.value, "250.00")
                })
            })
            val freeBalanceCard = result.cards.first { it.key == OverviewDashboardCardKey.GOAL_FREE_BALANCE }
            assertThat(freeBalanceCard.details).hasSize(2)
            assertThat(freeBalanceCard.details[0].sourceType).isEqualTo(OverviewDashboardDetailSourceType.FORMULA)
            assertMoney(freeBalanceCard.details[0].value, "1000.00")
            assertThat(freeBalanceCard.details[1]).satisfies({ mainRow ->
                assertThat(mainRow.sourceId).isEqualTo(bankId)
                assertThat(mainRow.sourceType).isEqualTo(OverviewDashboardDetailSourceType.BANK_ACCOUNT)
                assertThat(mainRow.label).isEqualTo("Main")
                assertMoney(mainRow.value, "750.00")
                assertThat(mainRow.accountOverCommitted).isFalse()
                assertThat(mainRow.children).singleElement().satisfies({ child ->
                    assertThat(child.sourceType).isEqualTo(OverviewDashboardDetailSourceType.GOAL)
                    assertMoney(child.value, "-250.00")
                })
            })
        }

    @Test
    fun `should raise goal warning when one visible bank account is over-committed`() =
        runBlocking {
            val ownerUserId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val primaryBankId = UUID.randomUUID()
            val secondaryBankId = UUID.randomUUID()
            val goalId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            walletItemRepository.save(
                bankAccountEntity(
                    id = primaryBankId,
                    userId = ownerUserId,
                    name = "Primary",
                    currency = "BRL",
                    balance = "50.00",
                    showOnDashboard = true,
                ),
            )
            walletItemRepository.save(
                bankAccountEntity(
                    id = secondaryBankId,
                    userId = ownerUserId,
                    name = "Secondary",
                    currency = "BRL",
                    balance = "100.00",
                    showOnDashboard = true,
                ),
            )

            val walletEntryRepository =
                stubWalletEntryRepository(
                    monthlySummaries =
                        listOf(
                            monthly(primaryBankId, "2026-04", net = "0", cashIn = "0", cashOut = "0"),
                            monthly(secondaryBankId, "2026-04", net = "0", cashIn = "0", cashOut = "0"),
                        ),
                )

            val goalLedgerSummaryRepository =
                object : GoalLedgerCommittedSummaryRepository {
                    override fun summarizeCommittedByUserGoals(userId: UUID): Flux<GoalCommittedByWalletRow> =
                        if (userId == ownerUserId) {
                            Flux.just(
                                GoalCommittedByWalletRow(
                                    walletItemId = primaryBankId,
                                    currency = "BRL",
                                    committed = BigDecimal("80.00"),
                                ),
                                GoalCommittedByWalletRow(
                                    walletItemId = secondaryBankId,
                                    currency = "BRL",
                                    committed = BigDecimal("20.00"),
                                ),
                            )
                        } else {
                            Flux.empty()
                        }

                    override fun summarizeCommittedByUserGoalsDetailed(userId: UUID): Flux<GoalCommittedByGoalRow> =
                        if (userId == ownerUserId) {
                            Flux.just(
                                GoalCommittedByGoalRow(
                                    goalId = goalId,
                                    goalName = "Reserve",
                                    walletItemId = primaryBankId,
                                    currency = "BRL",
                                    committed = BigDecimal("80.00"),
                                ),
                                GoalCommittedByGoalRow(
                                    goalId = goalId,
                                    goalName = "Reserve",
                                    walletItemId = secondaryBankId,
                                    currency = "BRL",
                                    committed = BigDecimal("20.00"),
                                ),
                            )
                        } else {
                            Flux.empty()
                        }

                    override fun summarizeCommittedByGroupGoals(groupId: UUID): Flux<GoalCommittedByWalletRow> = Flux.empty()

                    override fun summarizeCommittedByGroupGoalsDetailed(groupId: UUID): Flux<GoalCommittedByGoalRow> = Flux.empty()

                    override fun summarizeCommittedByGoal(goalId: UUID): Flux<GoalCurrencyCommittedRow> = Flux.empty()
                }

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = fakeRecurrenceSimulationService(emptyList()),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                    goalLedgerSummaryRepository = goalLedgerSummaryRepository,
                )

            val result =
                service.getOverview(
                    userId = ownerUserId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.goalCommittedTotal, "100.00")
            assertMoney(result.freeBalanceTotal, "50.00")
            assertThat(result.goalOverCommittedWarning).isTrue()
            val committedCard = result.cards.first { it.key == OverviewDashboardCardKey.GOAL_COMMITTED }
            assertThat(committedCard.details).hasSize(2)
            assertThat(committedCard.details[0]).satisfies({ primary ->
                assertThat(primary.label).isEqualTo("Primary")
                assertThat(primary.sourceType).isEqualTo(OverviewDashboardDetailSourceType.BANK_ACCOUNT)
                assertThat(primary.accountOverCommitted).isTrue()
                assertMoney(primary.value, "80.00")
                assertThat(primary.children).singleElement().satisfies({ child ->
                    assertThat(child.label).isEqualTo("Reserve")
                    assertMoney(child.value, "80.00")
                })
            })
            assertThat(committedCard.details[1]).satisfies({ secondary ->
                assertThat(secondary.label).isEqualTo("Secondary")
                assertThat(secondary.accountOverCommitted).isFalse()
                assertMoney(secondary.value, "20.00")
                assertThat(secondary.children).singleElement().satisfies({ child ->
                    assertThat(child.label).isEqualTo("Reserve")
                    assertMoney(child.value, "20.00")
                })
            })
            val freeCard = result.cards.first { it.key == OverviewDashboardCardKey.GOAL_FREE_BALANCE }
            assertThat(freeCard.details).hasSize(3)
            assertThat(freeCard.details[0].sourceType).isEqualTo(OverviewDashboardDetailSourceType.FORMULA)
            assertMoney(freeCard.details[0].value, "150.00")
            assertThat(freeCard.details[1]).satisfies({ primary ->
                assertThat(primary.label).isEqualTo("Primary")
                assertThat(primary.accountOverCommitted).isTrue()
                assertMoney(primary.value, "-30.00")
                assertThat(primary.children).singleElement().satisfies({ child ->
                    assertMoney(child.value, "-80.00")
                })
            })
            assertThat(freeCard.details[2]).satisfies({ secondary ->
                assertThat(secondary.label).isEqualTo("Secondary")
                assertThat(secondary.accountOverCommitted).isFalse()
                assertMoney(secondary.value, "80.00")
                assertThat(secondary.children).singleElement().satisfies({ child ->
                    assertMoney(child.value, "-20.00")
                })
            })
        }

    @Test
    fun `should build future balance chart points using end-of-period projected balance semantics`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 6)
            val bankId = UUID.randomUUID()
            val walletItemRepository = InMemoryWalletItemRepository()
            val bankEntity =
                bankAccountEntity(
                    id = bankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                )
            walletItemRepository.save(bankEntity)
            val bankWallet = walletFromEntity(bankEntity)

            val walletEntryRepository =
                stubWalletEntryRepository(
                    monthlySummaries =
                        listOf(
                            monthly(
                                walletItemId = bankId,
                                month = "2026-04",
                                net = "80.00",
                                cashIn = "120.00",
                                cashOut = "40.00",
                            ),
                        ),
                )

            val projectedEvents =
                listOf(
                    simulatedEvent(
                        date = LocalDate.of(2026, 4, 20),
                        walletItemId = bankId,
                        walletItem = bankWallet,
                        value = "25.00",
                    ),
                    simulatedEvent(
                        date = LocalDate.of(2026, 5, 5),
                        walletItemId = bankId,
                        walletItem = bankWallet,
                        value = "-10.00",
                    ),
                    simulatedEvent(
                        date = LocalDate.of(2026, 6, 10),
                        walletItemId = bankId,
                        walletItem = bankWallet,
                        value = "-5.00",
                    ),
                )

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = fakeRecurrenceSimulationService(projectedEvents),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertThat(result.charts.balance).hasSize(12)
            assertThat(
                result.charts.balance
                    .last()
                    .month,
            ).isEqualTo(selectedMonth)

            val aprilBalance = result.balancePoint("2026-04")
            assertMoney(aprilBalance.executedValue, "1000.00")
            assertMoney(aprilBalance.projectedValue, "25.00")
            assertMoney(aprilBalance.value, "1025.00")

            val aprilCashIn = result.cashInPoint("2026-04")
            assertMoney(aprilCashIn.executedValue, "120.00")
            assertMoney(aprilCashIn.projectedValue, "25.00")
            assertMoney(aprilCashIn.value, "145.00")

            val juneBalance = result.balancePoint("2026-06")
            assertMoney(juneBalance.executedValue, "1000.00")
            assertMoney(juneBalance.projectedValue, "10.00")
            assertMoney(juneBalance.value, "1010.00")

            val juneCashOut = result.cashOutPoint("2026-06")
            assertMoney(juneCashOut.executedValue, "0.00")
            assertMoney(juneCashOut.projectedValue, "5.00")
            assertMoney(juneCashOut.value, "5.00")
        }

    @Test
    fun `should include projected credit card bill cash out in future balance chart point`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 5)
            val bankId = UUID.randomUUID()
            val creditCardId = UUID.randomUUID()
            val walletItemRepository = InMemoryWalletItemRepository()

            val bankEntity =
                bankAccountEntity(
                    id = bankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                )
            val creditCard =
                creditCardEntity(
                    id = creditCardId,
                    userId = userId,
                    name = "Card",
                    currency = "BRL",
                    totalLimit = "5000.00",
                    availableLimit = "4000.00",
                    showOnDashboard = true,
                )
            walletItemRepository.save(bankEntity)
            walletItemRepository.save(creditCard)

            val bankWallet = walletFromEntity(bankEntity)

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = stubWalletEntryRepository(emptyList()),
                    recurrenceSimulationService =
                        fakeRecurrenceSimulationService(
                            listOf(
                                simulatedEvent(
                                    date = LocalDate.of(2026, 5, 20),
                                    walletItemId = bankId,
                                    walletItem = bankWallet,
                                    value = "50.00",
                                ),
                                simulatedCreditCardEvent(
                                    date = LocalDate.of(2026, 5, 12),
                                    walletItemId = creditCardId,
                                    walletItem = walletFromEntity(creditCard),
                                    value = "-30.00",
                                    billDate = LocalDate.of(2026, 5, 1),
                                ),
                            ),
                        ),
                    creditCardBillService =
                        fakeCreditCardBillService(
                            mapOf(
                                creditCardId to
                                    bill(
                                        creditCardId = creditCardId,
                                        value = "-100.00",
                                        paid = false,
                                        billDate = LocalDate.of(2026, 5, 1),
                                        dueDate = LocalDate.of(2026, 5, 10),
                                        closingDate = LocalDate.of(2026, 5, 3),
                                    ),
                            ),
                        ),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_OUT), "130.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.END_OF_PERIOD_BALANCE), "920.00")

            val mayBalance = result.balancePoint("2026-05")
            assertMoney(mayBalance.executedValue, "1000.00")
            assertMoney(mayBalance.projectedValue, "-80.00")
            assertMoney(mayBalance.value, "920.00")
        }

    @Test
    fun `should include projected credit card bill transactions in projected cash out`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val creditCardId = UUID.randomUUID()
            val walletItemRepository = InMemoryWalletItemRepository()
            val creditCardEntity =
                creditCardEntity(
                    id = creditCardId,
                    userId = userId,
                    name = "Card",
                    currency = "BRL",
                    totalLimit = "5000.00",
                    availableLimit = "4000.00",
                    showOnDashboard = true,
                )
            walletItemRepository.save(creditCardEntity)
            val creditCardWallet = walletFromEntity(creditCardEntity)

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = stubWalletEntryRepository(emptyList()),
                    recurrenceSimulationService =
                        fakeRecurrenceSimulationService(
                            listOf(
                                simulatedCreditCardEvent(
                                    date = LocalDate.of(2026, 4, 20),
                                    walletItemId = creditCardId,
                                    walletItem = creditCardWallet,
                                    value = "-30.00",
                                    billDate = LocalDate.of(2026, 4, 1),
                                ),
                                simulatedCreditCardEvent(
                                    date = LocalDate.of(2026, 4, 22),
                                    walletItemId = creditCardId,
                                    walletItem = creditCardWallet,
                                    value = "-40.00",
                                    billDate = LocalDate.of(2026, 5, 1),
                                ),
                            ),
                        ),
                    creditCardBillService =
                        fakeCreditCardBillService(
                            mapOf(
                                creditCardId to bill(creditCardId = creditCardId, value = "-100.00", paid = false),
                            ),
                        ),
                    exchangeRateService = identityExchangeRateService(),
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            val projectedCashOutCard = result.cards.first { it.key == OverviewDashboardCardKey.PROJECTED_CASH_OUT }
            assertMoney(projectedCashOutCard.value, "130.00")
            assertThat(projectedCashOutCard.details).singleElement().satisfies({ detail ->
                assertThat(detail.sourceType).isEqualTo(OverviewDashboardDetailSourceType.CREDIT_CARD_BILL)
                assertThat(detail.label).isEqualTo("Card")
                assertMoney(detail.value, "130.00")
            })
            val aprilCashOut = result.cashOutPoint("2026-04")
            assertMoney(aprilCashOut.executedValue, "0.00")
            assertMoney(aprilCashOut.projectedValue, "130.00")
            assertMoney(aprilCashOut.value, "130.00")

            val aprilExpense = result.expensePoint("2026-04")
            assertMoney(aprilExpense.executedValue, "0.00")
            assertMoney(aprilExpense.projectedValue, "70.00")
            assertMoney(aprilExpense.value, "70.00")
            assertMoney(result.groupSliceValue("PREDEFINED_INDIVIDUAL"), "70.00")
            assertMoney(result.groupSlice("PREDEFINED_INDIVIDUAL").executedValue, "0.00")
            assertMoney(result.groupSlice("PREDEFINED_INDIVIDUAL").projectedValue, "70.00")
            assertMoney(result.categorySliceValue("PREDEFINED_UNCATEGORIZED"), "70.00")
            assertMoney(result.categorySlice("PREDEFINED_UNCATEGORIZED").executedValue, "0.00")
            assertMoney(result.categorySlice("PREDEFINED_UNCATEGORIZED").projectedValue, "70.00")
        }

    @Test
    fun `should include projected recurring credit card expense for future selected month`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 5)
            val creditCardId = UUID.randomUUID()
            val walletItemRepository = InMemoryWalletItemRepository()
            val creditCardEntity =
                creditCardEntity(
                    id = creditCardId,
                    userId = userId,
                    name = "Card",
                    currency = "BRL",
                    totalLimit = "5000.00",
                    availableLimit = "4000.00",
                    showOnDashboard = true,
                )
            walletItemRepository.save(creditCardEntity)
            val creditCardWallet = walletFromEntity(creditCardEntity)

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = stubWalletEntryRepository(emptyList()),
                    recurrenceSimulationService =
                        fakeRecurrenceSimulationService(
                            listOf(
                                simulatedCreditCardEvent(
                                    date = LocalDate.of(2026, 5, 12),
                                    walletItemId = creditCardId,
                                    walletItem = creditCardWallet,
                                    value = "-45.00",
                                    billDate = LocalDate.of(2026, 6, 1),
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

            val mayExpense = result.expensePoint("2026-05")
            assertMoney(mayExpense.executedValue, "0.00")
            assertMoney(mayExpense.projectedValue, "45.00")
            assertMoney(mayExpense.value, "45.00")
            assertMoney(result.groupSliceValue("PREDEFINED_INDIVIDUAL"), "45.00")
            assertMoney(result.groupSlice("PREDEFINED_INDIVIDUAL").executedValue, "0.00")
            assertMoney(result.groupSlice("PREDEFINED_INDIVIDUAL").projectedValue, "45.00")
            assertMoney(result.categorySliceValue("PREDEFINED_UNCATEGORIZED"), "45.00")
            assertMoney(result.categorySlice("PREDEFINED_UNCATEGORIZED").executedValue, "0.00")
            assertMoney(result.categorySlice("PREDEFINED_UNCATEGORIZED").projectedValue, "45.00")
        }

    @Test
    fun `should merge projected and executed values for cash breakdown categories`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val primaryBankId = UUID.randomUUID()
            val secondaryBankId = UUID.randomUUID()
            val walletItemRepository = InMemoryWalletItemRepository()
            val primaryBank =
                bankAccountEntity(
                    id = primaryBankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                )
            val secondaryBank =
                bankAccountEntity(
                    id = secondaryBankId,
                    userId = userId,
                    name = "Reserve",
                    currency = "BRL",
                    balance = "800.00",
                    showOnDashboard = true,
                )
            walletItemRepository.save(primaryBank)
            walletItemRepository.save(secondaryBank)
            val primaryWallet = walletFromEntity(primaryBank)
            val secondaryWallet = walletFromEntity(secondaryBank)

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository =
                        stubWalletEntryRepository(
                            monthlySummaries = emptyList(),
                            cashBreakdowns =
                                listOf(
                                    cashBreakdown(direction = OverviewCashDirection.IN, categoryName = "Salary", amount = "100.00"),
                                    cashBreakdown(direction = OverviewCashDirection.OUT, categoryName = "Bills", amount = "40.00"),
                                ),
                        ),
                    recurrenceSimulationService =
                        fakeRecurrenceSimulationService(
                            listOf(
                                simulatedEventWithCategory(
                                    date = LocalDate.of(2026, 4, 20),
                                    walletItemId = primaryBankId,
                                    walletItem = primaryWallet,
                                    value = "30.00",
                                    categoryName = "Salary",
                                ),
                                simulatedEventWithCategory(
                                    date = LocalDate.of(2026, 4, 21),
                                    walletItemId = primaryBankId,
                                    walletItem = primaryWallet,
                                    value = "-10.00",
                                    categoryName = "Bills",
                                ),
                                simulatedTransferEvent(
                                    date = LocalDate.of(2026, 4, 22),
                                    entries =
                                        listOf(
                                            simulatedEntry(
                                                walletItemId = primaryBankId,
                                                walletItem = primaryWallet,
                                                value = "-50.00",
                                            ),
                                            simulatedEntry(
                                                walletItemId = secondaryBankId,
                                                walletItem = secondaryWallet,
                                                value = "50.00",
                                            ),
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

            assertMoney(result.cashInCategorySlice("Salary").executedValue, "100.00")
            assertMoney(result.cashInCategorySlice("Salary").projectedValue, "30.00")
            assertMoney(result.cashInCategorySlice("Salary").value, "130.00")

            assertMoney(result.cashOutCategorySlice("Bills").executedValue, "40.00")
            assertMoney(result.cashOutCategorySlice("Bills").projectedValue, "10.00")
            assertMoney(result.cashOutCategorySlice("Bills").value, "50.00")
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

    @Test
    fun `should use shared current-month overview retrieval phases`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val visibleBankId = UUID.randomUUID()
            val visibleCardId = UUID.randomUUID()
            val hiddenBankId = UUID.randomUUID()

            val walletItemsDelegate = InMemoryWalletItemRepository()
            val visibleBankEntity =
                bankAccountEntity(
                    id = visibleBankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                )
            val visibleCardEntity =
                creditCardEntity(
                    id = visibleCardId,
                    userId = userId,
                    name = "Visa",
                    currency = "BRL",
                    totalLimit = "2000.00",
                    availableLimit = "1500.00",
                    showOnDashboard = true,
                )
            walletItemsDelegate.save(visibleBankEntity)
            walletItemsDelegate.save(visibleCardEntity)
            walletItemsDelegate.save(
                bankAccountEntity(
                    id = hiddenBankId,
                    userId = userId,
                    name = "Hidden",
                    currency = "BRL",
                    balance = "300.00",
                    showOnDashboard = false,
                ),
            )

            val walletItemRepository = CountingWalletItemRepository(walletItemsDelegate)
            val walletEntryRepository =
                CountingWalletEntryRepository(
                    stubWalletEntryRepository(
                        monthlySummaries = listOf(monthly(visibleBankId, "2026-04", net = "0.00", cashIn = "0.00", cashOut = "0.00")),
                        expenseMonthlySummaries = listOf(expenseByMonth(month = "2026-04", expense = "0.00")),
                    ),
                )
            val recurrenceSimulationService =
                CountingRecurrenceSimulationService(
                    listOf(
                        simulatedEventWithCategory(
                            date = LocalDate.of(2026, 4, 20),
                            walletItemId = visibleBankId,
                            walletItem = walletFromEntity(visibleBankEntity),
                            value = "-15.00",
                            categoryName = "Food",
                        ),
                        simulatedCreditCardEvent(
                            date = LocalDate.of(2026, 4, 21),
                            walletItemId = visibleCardId,
                            walletItem = walletFromEntity(visibleCardEntity),
                            value = "-25.00",
                            billDate = LocalDate.of(2026, 4, 1),
                        ),
                    ),
                )
            val creditCardBillService =
                CountingCreditCardBillService(
                    fakeCreditCardBillService(
                        mapOf(
                            visibleCardId to bill(creditCardId = visibleCardId, value = "-30.00", paid = false),
                        ),
                    ),
                )
            val goalLedgerSummaryRepository =
                CountingGoalLedgerSummaryRepository(
                    detailedRows =
                        listOf(
                            GoalCommittedByGoalRow(
                                goalId = UUID.randomUUID(),
                                goalName = "Reserve",
                                walletItemId = visibleBankId,
                                currency = "BRL",
                                committed = BigDecimal("10.00"),
                            ),
                        ),
                )
            val exchangeRateService = CountingExchangeRateService(identityExchangeRateService())

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = recurrenceSimulationService,
                    creditCardBillService = creditCardBillService,
                    exchangeRateService = exchangeRateService,
                    goalLedgerSummaryRepository = goalLedgerSummaryRepository,
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "USD",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_OUT), "70.00")
            assertMoney(result.goalCommittedTotal, "10.00")
            assertThat(walletItemRepository.visibleLookupCalls).isEqualTo(1)
            assertThat(walletItemRepository.enabledLookupCalls).isZero()
            assertThat(walletEntryRepository.bankFactCalls).isEqualTo(1)
            assertThat(walletEntryRepository.expenseFactCalls).isEqualTo(1)
            assertThat(walletEntryRepository.bankByMonthCalls).isZero()
            assertThat(walletEntryRepository.expenseByMonthCalls).isZero()
            assertThat(walletEntryRepository.expenseBySourceCalls).isZero()
            assertThat(walletEntryRepository.cashBreakdownCalls).isZero()
            assertThat(walletEntryRepository.expenseBreakdownCalls).isZero()
            assertThat(recurrenceSimulationService.filteredSimulationCalls).isEqualTo(1)
            assertThat(recurrenceSimulationService.unfilteredSimulationCalls).isZero()
            assertThat(creditCardBillService.openByDueDateBetweenCalls).isEqualTo(1)
            assertThat(goalLedgerSummaryRepository.detailedCalls).isEqualTo(1)
            assertThat(goalLedgerSummaryRepository.walletCalls).isZero()
            assertThat(exchangeRateService.convertBatchCalls).isEqualTo(1)
        }

    @Test
    fun `should skip projected retrieval phases for past-month overview`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 3)
            val visibleBankId = UUID.randomUUID()
            val visibleCardId = UUID.randomUUID()

            val walletItemsDelegate = InMemoryWalletItemRepository()
            walletItemsDelegate.save(
                bankAccountEntity(
                    id = visibleBankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                ),
            )
            walletItemsDelegate.save(
                creditCardEntity(
                    id = visibleCardId,
                    userId = userId,
                    name = "Visa",
                    currency = "BRL",
                    totalLimit = "2000.00",
                    availableLimit = "1500.00",
                    showOnDashboard = true,
                ),
            )

            val walletItemRepository = CountingWalletItemRepository(walletItemsDelegate)
            val walletEntryRepository =
                CountingWalletEntryRepository(
                    stubWalletEntryRepository(
                        monthlySummaries = listOf(monthly(visibleBankId, "2026-03", net = "0.00", cashIn = "0.00", cashOut = "0.00")),
                        expenseMonthlySummaries = listOf(expenseByMonth(month = "2026-03", expense = "0.00")),
                    ),
                )
            val recurrenceSimulationService =
                CountingRecurrenceSimulationService(
                    emptyList(),
                )
            val creditCardBillService =
                CountingCreditCardBillService(
                    fakeCreditCardBillService(
                        mapOf(
                            visibleCardId to bill(creditCardId = visibleCardId, value = "-30.00", paid = false),
                        ),
                    ),
                )

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = recurrenceSimulationService,
                    creditCardBillService = creditCardBillService,
                    exchangeRateService = identityExchangeRateService(),
                )

            service.getOverview(
                userId = userId,
                defaultCurrency = "BRL",
                selectedMonth = selectedMonth,
            )

            assertThat(walletItemRepository.visibleLookupCalls).isEqualTo(1)
            assertThat(recurrenceSimulationService.filteredSimulationCalls).isZero()
            assertThat(recurrenceSimulationService.unfilteredSimulationCalls).isZero()
            assertThat(creditCardBillService.openByDueDateBetweenCalls).isZero()
        }

    @Test
    fun `should use shared future-month overview retrieval phases`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 6)
            val visibleBankId = UUID.randomUUID()
            val visibleCardId = UUID.randomUUID()

            val walletItemsDelegate = InMemoryWalletItemRepository()
            val visibleBankEntity =
                bankAccountEntity(
                    id = visibleBankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                )
            val visibleCardEntity =
                creditCardEntity(
                    id = visibleCardId,
                    userId = userId,
                    name = "Visa",
                    currency = "BRL",
                    totalLimit = "2000.00",
                    availableLimit = "1500.00",
                    showOnDashboard = true,
                )
            walletItemsDelegate.save(visibleBankEntity)
            walletItemsDelegate.save(visibleCardEntity)

            val walletItemRepository = CountingWalletItemRepository(walletItemsDelegate)
            val walletEntryRepository =
                CountingWalletEntryRepository(
                    stubWalletEntryRepository(
                        monthlySummaries = listOf(monthly(visibleBankId, "2026-04", net = "0.00", cashIn = "0.00", cashOut = "0.00")),
                        expenseMonthlySummaries = listOf(expenseByMonth(month = "2026-04", expense = "0.00")),
                    ),
                )
            val recurrenceSimulationService =
                CountingRecurrenceSimulationService(
                    listOf(
                        simulatedEventWithCategory(
                            date = LocalDate.of(2026, 5, 20),
                            walletItemId = visibleBankId,
                            walletItem = walletFromEntity(visibleBankEntity),
                            value = "-15.00",
                            categoryName = "Food",
                        ),
                        simulatedCreditCardEvent(
                            date = LocalDate.of(2026, 6, 21),
                            walletItemId = visibleCardId,
                            walletItem = walletFromEntity(visibleCardEntity),
                            value = "-25.00",
                            billDate = LocalDate.of(2026, 6, 1),
                        ),
                    ),
                )
            val creditCardBillService =
                CountingCreditCardBillService(
                    fakeCreditCardBillService(
                        mapOf(
                            visibleCardId to
                                bill(
                                    creditCardId = visibleCardId,
                                    value = "-30.00",
                                    paid = false,
                                    billDate = LocalDate.of(2026, 6, 1),
                                    dueDate = LocalDate.of(2026, 6, 10),
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
                    creditCardBillService = creditCardBillService,
                    exchangeRateService = identityExchangeRateService(),
                )

            service.getOverview(
                userId = userId,
                defaultCurrency = "BRL",
                selectedMonth = selectedMonth,
            )

            assertThat(walletItemRepository.visibleLookupCalls).isEqualTo(1)
            assertThat(recurrenceSimulationService.filteredSimulationCalls).isEqualTo(1)
            assertThat(recurrenceSimulationService.unfilteredSimulationCalls).isZero()
            assertThat(creditCardBillService.openByDueDateBetweenCalls).isEqualTo(1)
        }

    @Test
    fun `should preserve visibility and projected overview semantics under shared pipeline`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val visibleBankId = UUID.randomUUID()
            val hiddenBankId = UUID.randomUUID()
            val visibleCardId = UUID.randomUUID()
            val hiddenCardId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val visibleBankEntity =
                bankAccountEntity(
                    id = visibleBankId,
                    userId = userId,
                    name = "Main",
                    currency = "BRL",
                    balance = "1000.00",
                    showOnDashboard = true,
                )
            val hiddenBankEntity =
                bankAccountEntity(
                    id = hiddenBankId,
                    userId = userId,
                    name = "Hidden reserve",
                    currency = "BRL",
                    balance = "400.00",
                    showOnDashboard = false,
                )
            val visibleCardEntity =
                creditCardEntity(
                    id = visibleCardId,
                    userId = userId,
                    name = "Visa",
                    currency = "BRL",
                    totalLimit = "2000.00",
                    availableLimit = "1600.00",
                    showOnDashboard = true,
                )
            val hiddenCardEntity =
                creditCardEntity(
                    id = hiddenCardId,
                    userId = userId,
                    name = "Hidden card",
                    currency = "BRL",
                    totalLimit = "2000.00",
                    availableLimit = "1900.00",
                    showOnDashboard = false,
                )
            walletItemRepository.save(visibleBankEntity)
            walletItemRepository.save(hiddenBankEntity)
            walletItemRepository.save(visibleCardEntity)
            walletItemRepository.save(hiddenCardEntity)

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
                    listOf(
                        simulatedEventWithCategory(
                            date = LocalDate.of(2026, 4, 20),
                            walletItemId = visibleBankId,
                            walletItem = walletFromEntity(visibleBankEntity),
                            value = "-20.00",
                            categoryName = "Food",
                        ),
                        simulatedEventWithCategory(
                            date = LocalDate.of(2026, 4, 20),
                            walletItemId = hiddenBankId,
                            walletItem = walletFromEntity(hiddenBankEntity),
                            value = "-999.00",
                            categoryName = "Food",
                        ),
                        simulatedCreditCardEvent(
                            date = LocalDate.of(2026, 4, 21),
                            walletItemId = visibleCardId,
                            walletItem = walletFromEntity(visibleCardEntity),
                            value = "-40.00",
                            billDate = LocalDate.of(2026, 4, 1),
                        ),
                        simulatedCreditCardEvent(
                            date = LocalDate.of(2026, 4, 21),
                            walletItemId = hiddenCardId,
                            walletItem = walletFromEntity(hiddenCardEntity),
                            value = "-70.00",
                            billDate = LocalDate.of(2026, 4, 1),
                        ),
                        simulatedTransferEvent(
                            date = LocalDate.of(2026, 4, 22),
                            entries =
                                listOf(
                                    simulatedEntry(
                                        walletItemId = visibleBankId,
                                        walletItem = walletFromEntity(visibleBankEntity),
                                        value = "-30.00",
                                    ),
                                    simulatedEntry(
                                        walletItemId = hiddenBankId,
                                        walletItem = walletFromEntity(hiddenBankEntity),
                                        value = "30.00",
                                    ),
                                ),
                        ),
                    ),
                )
            val creditCardBillService =
                fakeCreditCardBillService(
                    mapOf(
                        visibleCardId to bill(creditCardId = visibleCardId, value = "-50.00", paid = false),
                        hiddenCardId to bill(creditCardId = hiddenCardId, value = "-30.00", paid = false),
                    ),
                )
            val goalLedgerSummaryRepository =
                object : GoalLedgerCommittedSummaryRepository {
                    override fun summarizeCommittedByUserGoals(userId: UUID): Flux<GoalCommittedByWalletRow> = Flux.empty()

                    override fun summarizeCommittedByUserGoalsDetailed(userId: UUID): Flux<GoalCommittedByGoalRow> =
                        Flux.just(
                            GoalCommittedByGoalRow(
                                goalId = UUID.randomUUID(),
                                goalName = "Reserve",
                                walletItemId = visibleBankId,
                                currency = "BRL",
                                committed = BigDecimal("250.00"),
                            ),
                            GoalCommittedByGoalRow(
                                goalId = UUID.randomUUID(),
                                goalName = "Hidden reserve",
                                walletItemId = hiddenBankId,
                                currency = "BRL",
                                committed = BigDecimal("100.00"),
                            ),
                        )

                    override fun summarizeCommittedByGroupGoals(groupId: UUID): Flux<GoalCommittedByWalletRow> = Flux.empty()

                    override fun summarizeCommittedByGroupGoalsDetailed(groupId: UUID): Flux<GoalCommittedByGoalRow> = Flux.empty()

                    override fun summarizeCommittedByGoal(goalId: UUID): Flux<GoalCurrencyCommittedRow> = Flux.empty()
                }

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = walletEntryRepository,
                    recurrenceSimulationService = recurrenceSimulationService,
                    creditCardBillService = creditCardBillService,
                    exchangeRateService = identityExchangeRateService(),
                    goalLedgerSummaryRepository = goalLedgerSummaryRepository,
                )

            val result =
                service.getOverview(
                    userId = userId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_OUT), "110.00")
            assertMoney(result.cardValue(OverviewDashboardCardKey.PROJECTED_EXPENSES), "60.00")
            assertMoney(result.goalCommittedTotal, "250.00")
            assertMoney(result.freeBalanceTotal, "750.00")
            assertThat(result.goalOverCommittedWarning).isFalse()
            assertThat(
                result.cards
                    .first { it.key == OverviewDashboardCardKey.PROJECTED_CASH_OUT }
                    .details
                    .map { it.label },
            ).contains("Main", "Visa")
                .doesNotContain("Hidden reserve", "Hidden card")
            assertMoney(result.categorySliceValue("Food"), "20.00")
            assertMoney(result.categorySliceValue(PREDEFINED_UNCATEGORIZED_LABEL), "40.00")
            assertMoney(result.groupSliceValue(PREDEFINED_INDIVIDUAL_LABEL), "60.00")
        }

    @Test
    fun `should include projected member debt in projected cards charts and expense category breakdown`() =
        runBlocking {
            val joaoUserId = UUID.randomUUID()
            val gabrielUserId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)
            val joaoBankId = UUID.randomUUID()
            val gabrielBankId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            walletItemRepository.save(
                bankAccountEntity(
                    id = joaoBankId,
                    userId = joaoUserId,
                    name = "Joao bank",
                    currency = "BRL",
                    balance = "0.00",
                    showOnDashboard = true,
                ),
            )
            walletItemRepository.save(
                bankAccountEntity(
                    id = gabrielBankId,
                    userId = gabrielUserId,
                    name = "Gabriel bank",
                    currency = "BRL",
                    balance = "0.00",
                    showOnDashboard = true,
                ),
            )

            val groupService =
                fakeGroupService(
                    groupsByUserId =
                        mapOf(
                            joaoUserId to listOf(groupWithRole(groupId, "Shared")),
                            gabrielUserId to listOf(groupWithRole(groupId, "Shared")),
                        ),
                )
            val groupDebtService =
                fakeGroupDebtService { requestedGroupId, scopedUserIds, fromMonth, toMonth ->
                    if (requestedGroupId != groupId || selectedMonth.isBefore(fromMonth) || selectedMonth.isAfter(toMonth)) {
                        return@fakeGroupDebtService emptyMap()
                    }

                    when {
                        scopedUserIds.contains(joaoUserId) ->
                            mapOf(
                                (selectedMonth to "BRL") to
                                    GroupDebtMonthlyCashFlow(
                                        debtOutflow = BigDecimal("49.00"),
                                        debtInflow = BigDecimal.ZERO,
                                    ),
                            )

                        scopedUserIds.contains(gabrielUserId) ->
                            mapOf(
                                (selectedMonth to "BRL") to
                                    GroupDebtMonthlyCashFlow(
                                        debtOutflow = BigDecimal.ZERO,
                                        debtInflow = BigDecimal("49.00"),
                                    ),
                            )

                        else -> emptyMap()
                    }
                }

            val service =
                createService(
                    clock = fixedClock("2026-04-15T12:00:00Z"),
                    walletItemRepository = walletItemRepository,
                    walletEntryRepository = stubWalletEntryRepository(emptyList()),
                    recurrenceSimulationService = fakeRecurrenceSimulationService(emptyList()),
                    creditCardBillService = fakeCreditCardBillService(emptyMap()),
                    exchangeRateService = identityExchangeRateService(),
                    groupService = groupService,
                    groupDebtService = groupDebtService,
                )

            val joaoOverview =
                service.getOverview(
                    userId = joaoUserId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )
            assertMoney(joaoOverview.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_OUT), "49.00")
            assertMoney(joaoOverview.cardValue(OverviewDashboardCardKey.PROJECTED_EXPENSES), "49.00")
            assertMoney(joaoOverview.cashOutPoint("2026-04").projectedValue, "49.00")
            assertMoney(joaoOverview.expensePoint("2026-04").projectedValue, "49.00")
            assertMoney(joaoOverview.categorySliceValue(PREDEFINED_SHARED_FINANCE_DEBT_LABEL), "49.00")
            assertThat(
                joaoOverview.cards
                    .first { it.key == OverviewDashboardCardKey.PROJECTED_CASH_OUT }
                    .details
                    .filter { it.sourceType == OverviewDashboardDetailSourceType.FORMULA },
            ).anySatisfy { detail ->
                assertThat(detail.label).isEqualTo(PROJECTED_DEBT_OUTFLOW_DETAIL_LABEL)
                assertMoney(detail.value, "49.00")
            }
            assertThat(
                joaoOverview.cards
                    .first { it.key == OverviewDashboardCardKey.PROJECTED_EXPENSES }
                    .details
                    .filter { it.sourceType == OverviewDashboardDetailSourceType.FORMULA },
            ).anySatisfy { detail ->
                assertThat(detail.label).isEqualTo(PROJECTED_DEBT_EXPENSE_DETAIL_LABEL)
                assertMoney(detail.value, "49.00")
            }

            val gabrielOverview =
                service.getOverview(
                    userId = gabrielUserId,
                    defaultCurrency = "BRL",
                    selectedMonth = selectedMonth,
                )
            assertMoney(gabrielOverview.cardValue(OverviewDashboardCardKey.PROJECTED_CASH_IN), "49.00")
            assertMoney(gabrielOverview.cashInPoint("2026-04").projectedValue, "49.00")
            assertThat(
                gabrielOverview.cards
                    .first { it.key == OverviewDashboardCardKey.PROJECTED_CASH_IN }
                    .details
                    .filter { it.sourceType == OverviewDashboardDetailSourceType.FORMULA },
            ).anySatisfy { detail ->
                assertThat(detail.label).isEqualTo(PROJECTED_DEBT_INFLOW_DETAIL_LABEL)
                assertMoney(detail.value, "49.00")
            }
        }

    private fun createService(
        clock: Clock,
        walletItemRepository: WalletItemRepository,
        walletEntryRepository: WalletEntryRepository,
        recurrenceSimulationService: RecurrenceSimulationService,
        creditCardBillService: CreditCardBillService,
        exchangeRateService: ExchangeRateService,
        walletEventListService: WalletEventListService = fakeWalletEventListService(),
        goalLedgerSummaryRepository: GoalLedgerCommittedSummaryRepository = NoOpGoalLedgerCommittedSummaryRepository,
        groupService: GroupService = NoOpGroupService(),
        groupDebtService: GroupDebtService = NoOpGroupDebtService,
    ): OverviewDashboardServiceImpl {
        val balanceService = OverviewDashboardBalanceServiceImpl()
        val dataService =
            OverviewDashboardDataServiceImpl(
                walletItemRepository = walletItemRepository,
                walletItemMapper = WalletItemMapperImpl(BankAccountMapperImpl(), CreditCardMapperImpl()),
                walletEntryRepository = walletEntryRepository,
                walletEventListService = walletEventListService,
                recurrenceSimulationService = recurrenceSimulationService,
                creditCardBillService = creditCardBillService,
                groupService = groupService,
                groupDebtService = groupDebtService,
                clock = clock,
            )
        val goalService = OverviewDashboardGoalServiceImpl(goalLedgerSummaryRepository)
        val contributionService = OverviewDashboardContributionServiceImpl(balanceService)
        val chartService = OverviewDashboardChartServiceImpl()
        val assemblyService = OverviewDashboardAssemblyServiceImpl(exchangeRateService)
        val groupOverviewBuilderService =
            GroupOverviewBuilderService(
                dataService = dataService,
                goalService = goalService,
                balanceService = balanceService,
                contributionService = contributionService,
                chartService = chartService,
                assemblyService = assemblyService,
                clock = clock,
            )

        return OverviewDashboardServiceImpl(
            dataService = dataService,
            balanceService = balanceService,
            contributionService = contributionService,
            goalService = goalService,
            assemblyService = assemblyService,
            cardService = OverviewDashboardCardServiceImpl(),
            chartService = chartService,
            groupOverviewBuilderService = groupOverviewBuilderService,
            clock = clock,
        )
    }

    private fun fakeGroupService(groupsByUserId: Map<UUID, List<GroupWithRole>>): GroupService {
        val delegate = NoOpGroupService()
        return object : GroupService by delegate {
            override suspend fun findAllGroups(userId: UUID): List<GroupWithRole> = groupsByUserId[userId].orEmpty()
        }
    }

    private fun groupWithRole(
        id: UUID,
        name: String,
    ): GroupWithRole =
        GroupWithRole(
            id = id,
            createdAt = null,
            updatedAt = null,
            name = name,
            role = UserGroupRole.EDITOR,
            itemsAssociated = null,
        ).also { group ->
            group.permissions = emptySet()
        }

    private fun fakeGroupDebtService(
        resolver: (UUID, Set<UUID>, YearMonth, YearMonth) -> Map<Pair<YearMonth, String>, GroupDebtMonthlyCashFlow>,
    ): GroupDebtService =
        object : GroupDebtService by NoOpGroupDebtService {
            override suspend fun loadMonthlyCashFlow(
                groupId: UUID,
                scopedUserIds: Set<UUID>,
                fromMonth: YearMonth,
                toMonth: YearMonth,
            ): Map<Pair<YearMonth, String>, GroupDebtMonthlyCashFlow> = resolver(groupId, scopedUserIds, fromMonth, toMonth)
        }

    private fun stubWalletEntryRepository(
        monthlySummaries: List<BankAccountMonthlySummary>,
        expenseMonthlySummaries: List<OverviewExpenseMonthlySummary> = emptyList(),
        cashBreakdowns: List<OverviewCashBreakdownSummary> = emptyList(),
        expenseBreakdowns: List<OverviewExpenseBreakdownSummary> = emptyList(),
        bankFacts: List<OverviewExecutedBankFactSummary> = emptyList(),
        expenseFacts: List<OverviewExecutedExpenseFactSummary> = emptyList(),
    ): WalletEntryRepository {
        val delegate = InMemoryWalletEntryRepository()
        val fallbackBreakdownMonth =
            monthlySummaries.maxOfOrNull { it.month }
                ?: expenseMonthlySummaries.maxOfOrNull { it.month }
                ?: YearMonth.of(2026, 4)

        val synthesizedBankFacts =
            if (bankFacts.isNotEmpty()) {
                bankFacts
            } else {
                monthlySummaries.map { summary ->
                    OverviewExecutedBankFactSummary(
                        walletItemId = summary.walletItemId,
                        month = summary.month,
                        categoryId = null,
                        categoryName = null,
                        currency = "BRL",
                        net = summary.net,
                        cashIn = summary.cashIn,
                        cashOut = summary.cashOut,
                    )
                } +
                    cashBreakdowns.map { breakdown ->
                        OverviewExecutedBankFactSummary(
                            walletItemId = UUID.nameUUIDFromBytes("cash-breakdown-wallet".toByteArray()),
                            month = fallbackBreakdownMonth,
                            categoryId = breakdown.categoryId,
                            categoryName = breakdown.categoryName,
                            currency = breakdown.currency,
                            net = BigDecimal.ZERO,
                            cashIn = if (breakdown.direction == OverviewCashDirection.IN) breakdown.amount else BigDecimal.ZERO,
                            cashOut = if (breakdown.direction == OverviewCashDirection.OUT) breakdown.amount else BigDecimal.ZERO,
                        )
                    }
            }

        val synthesizedExpenseFacts =
            if (expenseFacts.isNotEmpty()) {
                expenseFacts
            } else {
                val breakdownMonth = expenseMonthlySummaries.maxOfOrNull { it.month }
                val monthsCoveredByBreakdowns =
                    if (expenseBreakdowns.isEmpty() ||
                        breakdownMonth == null
                    ) {
                        emptySet()
                    } else {
                        setOf(breakdownMonth)
                    }

                expenseBreakdowns.map { breakdown ->
                    OverviewExecutedExpenseFactSummary(
                        month = breakdownMonth ?: fallbackBreakdownMonth,
                        walletItemId = UUID.nameUUIDFromBytes("expense-breakdown-wallet".toByteArray()),
                        walletItemName = "Expense source",
                        walletItemType = WalletItemType.BANK_ACCOUNT,
                        groupId = breakdown.groupId,
                        groupName = breakdown.groupName,
                        categoryId = breakdown.categoryId,
                        categoryName = breakdown.categoryName,
                        currency = breakdown.currency,
                        expense = breakdown.expense,
                    )
                } +
                    expenseMonthlySummaries
                        .filterNot { monthsCoveredByBreakdowns.contains(it.month) }
                        .map { summary ->
                            OverviewExecutedExpenseFactSummary(
                                month = summary.month,
                                walletItemId = UUID.nameUUIDFromBytes("expense-month-${summary.month}-${summary.currency}".toByteArray()),
                                walletItemName = "Expense source ${summary.month}",
                                walletItemType = WalletItemType.BANK_ACCOUNT,
                                groupId = null,
                                groupName = null,
                                categoryId = null,
                                categoryName = null,
                                currency = summary.currency,
                                expense = summary.expense,
                            )
                        }
            }

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
                asOfDate: LocalDate,
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

            override fun summarizeOverviewBankFacts(
                userId: UUID,
                minimumDate: LocalDate,
                maximumDate: LocalDate,
            ): Flux<OverviewExecutedBankFactSummary> =
                Flux.fromIterable(
                    synthesizedBankFacts.filter {
                        val date = it.month.atDay(1)
                        !date.isBefore(minimumDate) && !date.isAfter(maximumDate)
                    },
                )

            override fun summarizeOverviewExpenseFacts(
                userId: UUID,
                minimumDate: LocalDate,
                maximumDate: LocalDate,
            ): Flux<OverviewExecutedExpenseFactSummary> =
                Flux.fromIterable(
                    synthesizedExpenseFacts.filter {
                        val date = it.month.atDay(1)
                        !date.isBefore(minimumDate) && !date.isAfter(maximumDate)
                    },
                )

            override fun summarizeOverviewExpenseBySource(
                userId: UUID,
                minimumDate: LocalDate,
                maximumDate: LocalDate,
            ): Flux<OverviewExpenseSourceSummary> = Flux.empty()

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

    private class CountingWalletItemRepository(
        private val delegate: WalletItemRepository,
    ) : WalletItemRepository by delegate {
        var enabledLookupCalls: Int = 0
        var visibleLookupCalls: Int = 0

        override fun findAllByUserIdAndEnabled(
            userId: UUID,
            enabled: Boolean,
            pageable: org.springframework.data.domain.Pageable,
        ) = delegate.findAllByUserIdAndEnabled(userId, enabled, pageable).also { enabledLookupCalls += 1 }

        override fun findAllByUserIdAndEnabledAndShowOnDashboard(
            userId: UUID,
            enabled: Boolean,
            showOnDashboard: Boolean,
            pageable: org.springframework.data.domain.Pageable,
        ) = delegate
            .findAllByUserIdAndEnabledAndShowOnDashboard(userId, enabled, showOnDashboard, pageable)
            .also { visibleLookupCalls += 1 }
    }

    private class CountingWalletEntryRepository(
        private val delegate: WalletEntryRepository,
    ) : WalletEntryRepository by delegate {
        var bankByMonthCalls: Int = 0
        var bankFactCalls: Int = 0
        var expenseByMonthCalls: Int = 0
        var expenseFactCalls: Int = 0
        var expenseBySourceCalls: Int = 0
        var cashBreakdownCalls: Int = 0
        var expenseBreakdownCalls: Int = 0

        override fun summarizeBankAccountsByMonth(
            userId: UUID,
            minimumDate: LocalDate,
            maximumDate: LocalDate,
        ): Flux<BankAccountMonthlySummary> =
            delegate.summarizeBankAccountsByMonth(userId, minimumDate, maximumDate).also { bankByMonthCalls += 1 }

        override fun summarizeOverviewBankFacts(
            userId: UUID,
            minimumDate: LocalDate,
            maximumDate: LocalDate,
        ): Flux<OverviewExecutedBankFactSummary> =
            delegate.summarizeOverviewBankFacts(userId, minimumDate, maximumDate).also { bankFactCalls += 1 }

        override fun summarizeOverviewExpenseByMonth(
            userId: UUID,
            minimumDate: LocalDate,
            maximumDate: LocalDate,
        ): Flux<OverviewExpenseMonthlySummary> =
            delegate.summarizeOverviewExpenseByMonth(userId, minimumDate, maximumDate).also { expenseByMonthCalls += 1 }

        override fun summarizeOverviewExpenseFacts(
            userId: UUID,
            minimumDate: LocalDate,
            maximumDate: LocalDate,
        ): Flux<OverviewExecutedExpenseFactSummary> =
            delegate.summarizeOverviewExpenseFacts(userId, minimumDate, maximumDate).also { expenseFactCalls += 1 }

        override fun summarizeOverviewExpenseBySource(
            userId: UUID,
            minimumDate: LocalDate,
            maximumDate: LocalDate,
        ): Flux<OverviewExpenseSourceSummary> =
            delegate.summarizeOverviewExpenseBySource(userId, minimumDate, maximumDate).also { expenseBySourceCalls += 1 }

        override fun summarizeOverviewCashBreakdown(
            userId: UUID,
            minimumDate: LocalDate,
            maximumDate: LocalDate,
        ): Flux<OverviewCashBreakdownSummary> =
            delegate.summarizeOverviewCashBreakdown(userId, minimumDate, maximumDate).also { cashBreakdownCalls += 1 }

        override fun summarizeOverviewExpenseBreakdown(
            userId: UUID,
            minimumDate: LocalDate,
            maximumDate: LocalDate,
        ): Flux<OverviewExpenseBreakdownSummary> =
            delegate.summarizeOverviewExpenseBreakdown(userId, minimumDate, maximumDate).also { expenseBreakdownCalls += 1 }
    }

    private fun fakeWalletEventListService(): WalletEventListService =
        object : WalletEventListService {
            override suspend fun list(
                userId: UUID,
                request: ListEntryRequest,
            ): CursorPage<EventListResponse> =
                CursorPage(
                    items = emptyList(),
                    nextCursor = null,
                    hasNext = false,
                )

            override suspend fun findById(
                userId: UUID,
                walletEventId: UUID,
            ): EventListResponse? = null

            override suspend fun findScheduledByRecurrenceConfigId(
                userId: UUID,
                recurrenceConfigId: UUID,
            ): EventListResponse? = null

            override suspend fun convertEntityToEntryListResponse(
                events: List<com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity>,
                simulateBillForRecurrence: Boolean,
            ): List<EventListResponse> = emptyList()

            override suspend fun convertEntityToEntryListResponse(
                event: com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity,
                simulateBillForRecurrence: Boolean,
            ): EventListResponse = error("not used")
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
                groupIds: Set<UUID>,
                userIds: Set<UUID>,
                walletItemId: UUID?,
                billDate: LocalDate?,
                categoryConceptIds: Set<UUID>,
                includeUncategorized: Boolean,
            ): List<EventListResponse> =
                events.filter {
                    (minimumEndExecution == null || !it.date.isBefore(minimumEndExecution)) &&
                        (maximumNextExecution == null || !it.date.isAfter(maximumNextExecution))
                }

            override suspend fun simulateGenerationForUsers(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userIds: Set<UUID>,
                billDate: LocalDate?,
            ): List<EventListResponse> =
                events.filter {
                    (minimumEndExecution == null || !it.date.isBefore(minimumEndExecution)) &&
                        (maximumNextExecution == null || !it.date.isAfter(maximumNextExecution))
                }

            override suspend fun simulateGenerationWithFilters(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userId: UUID?,
                walletItemId: UUID?,
                billDate: LocalDate?,
                groupIds: Set<UUID>,
                userIds: Set<UUID>,
                walletItemIds: Set<UUID>,
                entryTypes: Set<WalletEntryType>,
                categoryConceptIds: Set<UUID>,
                includeUncategorized: Boolean,
            ): List<EventListResponse> =
                events.filter { event ->
                    (minimumEndExecution == null || !event.date.isBefore(minimumEndExecution)) &&
                        (maximumNextExecution == null || !event.date.isAfter(maximumNextExecution)) &&
                        (walletItemIds.isEmpty() || event.entries.any { entry -> walletItemIds.contains(entry.walletItemId) }) &&
                        (entryTypes.isEmpty() || entryTypes.contains(event.type))
                }

            override suspend fun simulateGenerationAsEntrySumResult(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userId: UUID?,
                groupId: UUID?,
                walletItemId: UUID?,
                summaryMinimumDate: LocalDate,
            ): List<EntrySumResult> = emptyList()

            override suspend fun simulateGenerationForCreditCard(
                billDate: LocalDate,
                userId: UUID,
                groupIds: Set<UUID>,
                userIds: Set<UUID>,
                walletItemId: UUID,
            ): List<EventListResponse> = emptyList()

            override suspend fun simulateGenerationForCreditCard(
                bill: CreditCardBill,
                userId: UUID,
                groupIds: Set<UUID>,
                userIds: Set<UUID>,
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

    private class CountingRecurrenceSimulationService(
        private val events: List<EventListResponse>,
    ) : RecurrenceSimulationService {
        var unfilteredSimulationCalls: Int = 0
        var filteredSimulationCalls: Int = 0

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
            groupIds: Set<UUID>,
            userIds: Set<UUID>,
            walletItemId: UUID?,
            billDate: LocalDate?,
            categoryConceptIds: Set<UUID>,
            includeUncategorized: Boolean,
        ): List<EventListResponse> {
            unfilteredSimulationCalls += 1
            return events.filter {
                (minimumEndExecution == null || !it.date.isBefore(minimumEndExecution)) &&
                    (maximumNextExecution == null || !it.date.isAfter(maximumNextExecution))
            }
        }

        override suspend fun simulateGenerationWithFilters(
            minimumEndExecution: LocalDate?,
            maximumNextExecution: LocalDate?,
            userId: UUID?,
            walletItemId: UUID?,
            billDate: LocalDate?,
            groupIds: Set<UUID>,
            userIds: Set<UUID>,
            walletItemIds: Set<UUID>,
            entryTypes: Set<WalletEntryType>,
            categoryConceptIds: Set<UUID>,
            includeUncategorized: Boolean,
        ): List<EventListResponse> {
            filteredSimulationCalls += 1
            return events.filter { event ->
                (minimumEndExecution == null || !event.date.isBefore(minimumEndExecution)) &&
                    (maximumNextExecution == null || !event.date.isAfter(maximumNextExecution)) &&
                    (walletItemIds.isEmpty() || event.entries.any { entry -> walletItemIds.contains(entry.walletItemId) }) &&
                    (entryTypes.isEmpty() || entryTypes.contains(event.type))
            }
        }

        override suspend fun simulateGenerationForUsers(
            minimumEndExecution: LocalDate?,
            maximumNextExecution: LocalDate?,
            userIds: Set<UUID>,
            billDate: LocalDate?,
        ): List<EventListResponse> =
            simulateGeneration(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                userId = null,
                groupIds = emptySet(),
                userIds = userIds,
                walletItemId = null,
                billDate = billDate,
                categoryConceptIds = emptySet(),
                includeUncategorized = false,
            )

        override suspend fun simulateGenerationAsEntrySumResult(
            minimumEndExecution: LocalDate?,
            maximumNextExecution: LocalDate?,
            userId: UUID?,
            groupId: UUID?,
            walletItemId: UUID?,
            summaryMinimumDate: LocalDate,
        ): List<EntrySumResult> = emptyList()

        override suspend fun simulateGenerationForCreditCard(
            billDate: LocalDate,
            userId: UUID,
            groupIds: Set<UUID>,
            userIds: Set<UUID>,
            walletItemId: UUID,
        ): List<EventListResponse> = emptyList()

        override suspend fun simulateGenerationForCreditCard(
            bill: CreditCardBill,
            userId: UUID,
            groupIds: Set<UUID>,
            userIds: Set<UUID>,
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

    private class CountingCreditCardBillService(
        private val delegate: CreditCardBillService,
    ) : CreditCardBillService by delegate {
        var openByDueDateBetweenCalls: Int = 0

        override suspend fun findAllOpenByDueDateBetween(
            userId: UUID,
            minimumDueDate: LocalDate,
            maximumDueDate: LocalDate,
        ): List<CreditCardBill> {
            openByDueDateBetweenCalls += 1
            return delegate.findAllOpenByDueDateBetween(userId, minimumDueDate, maximumDueDate)
        }
    }

    private class CountingGoalLedgerSummaryRepository(
        private val detailedRows: List<GoalCommittedByGoalRow>,
        private val walletRows: List<GoalCommittedByWalletRow> = emptyList(),
    ) : GoalLedgerCommittedSummaryRepository {
        var detailedCalls: Int = 0
        var walletCalls: Int = 0

        override fun summarizeCommittedByUserGoals(userId: UUID): Flux<GoalCommittedByWalletRow> {
            walletCalls += 1
            return Flux.fromIterable(walletRows)
        }

        override fun summarizeCommittedByUserGoalsDetailed(userId: UUID): Flux<GoalCommittedByGoalRow> {
            detailedCalls += 1
            return Flux.fromIterable(detailedRows)
        }

        override fun summarizeCommittedByGroupGoals(groupId: UUID): Flux<GoalCommittedByWalletRow> = Flux.empty()

        override fun summarizeCommittedByGroupGoalsDetailed(groupId: UUID): Flux<GoalCommittedByGoalRow> = Flux.empty()

        override fun summarizeCommittedByGoal(goalId: UUID): Flux<GoalCurrencyCommittedRow> = Flux.empty()
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

    private class CountingExchangeRateService(
        private val delegate: ExchangeRateService,
    ) : ExchangeRateService by delegate {
        var convertBatchCalls: Int = 0

        override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> {
            convertBatchCalls += 1
            return delegate.convertBatch(requests)
        }
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

    private fun simulatedEventWithCategory(
        date: LocalDate,
        walletItemId: UUID,
        walletItem: WalletItem,
        value: String,
        categoryName: String,
    ): EventListResponse =
        EventListResponse(
            id = UUID.randomUUID(),
            type = if (BigDecimal(value) >= BigDecimal.ZERO) WalletEntryType.REVENUE else WalletEntryType.EXPENSE,
            name = "simulated",
            category = categoryEntity(categoryName),
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

    private fun simulatedCreditCardEvent(
        date: LocalDate,
        walletItemId: UUID,
        walletItem: WalletItem,
        value: String,
        billDate: LocalDate,
    ): EventListResponse =
        EventListResponse(
            id = UUID.randomUUID(),
            type = com.ynixt.sharedfinances.domain.enums.WalletEntryType.EXPENSE,
            name = "simulated-card",
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
            entries =
                listOf(
                    EventListResponse.EntryResponse(
                        value = BigDecimal(value),
                        walletItem = walletItem,
                        walletItemId = walletItemId,
                        billDate = billDate,
                        billId = null,
                    ),
                ),
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

    private fun categoryEntity(name: String): com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity =
        com.ynixt.sharedfinances.domain.entities.wallet.entries
            .WalletEntryCategoryEntity(
                name = name,
                color = "#334155",
                userId = null,
                groupId = null,
                parentId = null,
                conceptId = UUID.nameUUIDFromBytes("concept-$name".toByteArray()),
            ).also { it.id = UUID.nameUUIDFromBytes(name.toByteArray()) }

    private fun bill(
        creditCardId: UUID,
        value: String,
        paid: Boolean,
        billDate: LocalDate = LocalDate.of(2026, 4, 1),
        dueDate: LocalDate = LocalDate.of(2026, 4, 10),
        closingDate: LocalDate = LocalDate.of(2026, 4, 3),
    ): CreditCardBill =
        CreditCardBill(
            id = UUID.randomUUID(),
            creditCardId = creditCardId,
            billDate = billDate,
            dueDate = dueDate,
            closingDate = closingDate,
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

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.balancePoint(month: String) =
        charts.balance.first { it.month == YearMonth.parse(month) }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.cashInPoint(month: String) =
        charts.cashIn.first { it.month == YearMonth.parse(month) }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.cashOutPoint(month: String) =
        charts.cashOut.first { it.month == YearMonth.parse(month) }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.expensePoint(month: String) =
        charts.expense.first { it.month == YearMonth.parse(month) }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.groupSliceValue(label: String): BigDecimal =
        groupSlice(label).value

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.categorySliceValue(label: String): BigDecimal =
        categorySlice(label).value

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.groupSlice(label: String) =
        charts.expenseByGroup.first { it.label == label }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.categorySlice(label: String) =
        charts.expenseByCategory.first { it.label == label }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.cashInCategorySlice(label: String) =
        charts.cashInByCategory.first { it.label == label }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.cashOutCategorySlice(label: String) =
        charts.cashOutByCategory.first { it.label == label }

    private fun com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard.expenseValue(month: String): BigDecimal =
        charts.expense.first { it.month == YearMonth.parse(month) }.value
}
