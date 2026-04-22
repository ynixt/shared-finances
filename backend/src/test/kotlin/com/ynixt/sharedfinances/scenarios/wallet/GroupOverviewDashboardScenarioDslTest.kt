package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyComposition
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtPairBalance
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtWorkspace
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletBeneficiaryLeg
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import com.ynixt.sharedfinances.scenarios.support.NoOpGroupDebtService
import com.ynixt.sharedfinances.scenarios.support.ScenarioGroupService
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import com.ynixt.sharedfinances.domain.enums.PaymentType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class GroupOverviewDashboardScenarioDslTest {
    @Test
    fun `group overview cards should honor group scope showOnDashboard and projected semantics`() {
        val today = LocalDate.of(2026, 4, 10)
        val selectedMonth = YearMonth.of(2026, 4)
        val groupService = ScenarioGroupService()
        val targetGroupId = groupService.createGroup(name = "Target Group")
        val otherGroupId = groupService.createGroup(name = "Other Group")

        lateinit var actorUserId: UUID
        lateinit var actorVisibleBankId: UUID
        lateinit var actorHiddenBankId: UUID
        lateinit var secondUserId: UUID
        lateinit var secondUserBankId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                actorUserId = user(email = "actor@example.com", firstName = "Actor", lastName = "User", defaultCurrency = "BRL")
                actorVisibleBankId = bankAccount(name = "Actor visible", balance = 1200, currency = "BRL", showOnDashboard = true)
                actorHiddenBankId = bankAccount(name = "Actor hidden", balance = 700, currency = "BRL", showOnDashboard = false)

                secondUserId = user(email = "member@example.com", firstName = "Member", lastName = "User", defaultCurrency = "BRL")
                secondUserBankId = bankAccount(name = "Member bank", balance = 400, currency = "BRL", showOnDashboard = true)
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = targetGroupId,
                    userId = actorUserId,
                    associatedItemIds = setOf(actorVisibleBankId, actorHiddenBankId, secondUserBankId),
                )
                groupService.upsertMemberScope(
                    groupId = targetGroupId,
                    userId = secondUserId,
                    associatedItemIds = setOf(actorVisibleBankId, actorHiddenBankId, secondUserBankId),
                )
                groupService.upsertMemberScope(
                    groupId = otherGroupId,
                    userId = actorUserId,
                    associatedItemIds = setOf(actorVisibleBankId),
                )

                switchUser(actorUserId)
                revenue(
                    value = 100,
                    originId = actorVisibleBankId,
                    groupId = targetGroupId,
                    date = LocalDate.of(2026, 4, 5),
                    name = "Target revenue executed",
                )
                expense(
                    value = 40,
                    originId = actorVisibleBankId,
                    groupId = targetGroupId,
                    date = LocalDate.of(2026, 4, 6),
                    name = "Target expense executed",
                )
                revenue(
                    value = 70,
                    originId = actorVisibleBankId,
                    groupId = targetGroupId,
                    date = LocalDate.of(2026, 4, 20),
                    name = "Target revenue projected",
                )
                expense(
                    value = 30,
                    originId = actorVisibleBankId,
                    groupId = targetGroupId,
                    date = LocalDate.of(2026, 4, 21),
                    name = "Target expense projected",
                )
                expense(
                    value = 999,
                    originId = actorHiddenBankId,
                    groupId = targetGroupId,
                    date = LocalDate.of(2026, 4, 7),
                    name = "Hidden dashboard item expense",
                )
                expense(
                    value = 888,
                    originId = actorVisibleBankId,
                    groupId = otherGroupId,
                    date = LocalDate.of(2026, 4, 8),
                    name = "Other group expense",
                )

                fetchGroupOverview(groupId = targetGroupId, selectedMonth = selectedMonth)
            }

            then {
                groupOverviewCardShouldBe(OverviewDashboardCardKey.PERIOD_CASH_IN, 100)
                groupOverviewCardShouldBe(OverviewDashboardCardKey.PERIOD_EXPENSES, 40)
                groupOverviewCardShouldBe(OverviewDashboardCardKey.PROJECTED_CASH_IN, 70)
                groupOverviewCardShouldBe(OverviewDashboardCardKey.PROJECTED_EXPENSES, 30)
                groupOverviewCashInForMonthShouldBe(month = selectedMonth, executed = 100, projected = 70)
                groupOverviewExpenseForMonthShouldBe(month = selectedMonth, executed = 40, projected = 30)
            }
        }
    }

    @Test
    fun `group overview charts should ignore debt-settlement movements on non-debt series and slices`() {
        val today = LocalDate.of(2026, 4, 10)
        val selectedMonth = YearMonth.of(2026, 4)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")

        lateinit var userAId: UUID
        lateinit var userABankId: UUID
        lateinit var userBId: UUID
        lateinit var userBBankId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                userAId = user(email = "user-a@example.com", firstName = "User", lastName = "A", defaultCurrency = "BRL")
                userABankId = bankAccount(name = "User A bank", balance = 1000, currency = "BRL", showOnDashboard = true)
                userBId = user(email = "user-b@example.com", firstName = "User", lastName = "B", defaultCurrency = "BRL")
                userBBankId = bankAccount(name = "User B bank", balance = 500, currency = "BRL", showOnDashboard = true)
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = userAId,
                    associatedItemIds = setOf(userABankId, userBBankId),
                )
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = userBId,
                    associatedItemIds = setOf(userABankId, userBBankId),
                )

                switchUser(userAId)
                revenue(value = 60, originId = userABankId, groupId = groupId, date = LocalDate.of(2026, 4, 5), name = "Executed revenue")
                expense(value = 80, originId = userABankId, groupId = groupId, date = LocalDate.of(2026, 4, 6), name = "Executed expense A")
                revenue(value = 10, originId = userABankId, groupId = groupId, date = LocalDate.of(2026, 4, 24), name = "Projected revenue")
                expense(value = 20, originId = userABankId, groupId = groupId, date = LocalDate.of(2026, 4, 25), name = "Projected expense")

                switchUser(userBId)
                expense(value = 25, originId = userBBankId, groupId = groupId, date = LocalDate.of(2026, 4, 4), name = "Executed expense B")

                switchUser(userAId)
                transfer(
                    value = 50,
                    date = LocalDate.of(2026, 4, 7),
                    groupId = groupId,
                    originId = userABankId,
                    targetId = userBBankId,
                    name = "Debt settlement transfer",
                    transferPurpose = com.ynixt.sharedfinances.domain.enums.TransferPurpose.DEBT_SETTLEMENT,
                )

                fetchGroupOverview(groupId = groupId, selectedMonth = selectedMonth)
            }

            then {
                groupOverviewCashInForMonthShouldBe(month = selectedMonth, executed = 60, projected = 10)
                groupOverviewExpenseForMonthShouldBe(month = selectedMonth, executed = 105, projected = 20)
                groupOverviewExpenseCategorySliceShouldBe(label = "PREDEFINED_UNCATEGORIZED", expected = 125)
                groupOverviewExpenseCategoryByMemberSliceShouldBe(
                    memberId = userAId,
                    label = "PREDEFINED_UNCATEGORIZED",
                    expected = 100,
                )
                groupOverviewExpenseCategoryByMemberSliceShouldBe(
                    memberId = userBId,
                    label = "PREDEFINED_UNCATEGORIZED",
                    expected = 25,
                )
            }
        }
    }

    @Test
    fun `group overview expense member charts should split by beneficiaries including projected values`() {
        val today = LocalDate.of(2026, 4, 10)
        val selectedMonth = YearMonth.of(2026, 4)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")

        lateinit var gabrielId: UUID
        lateinit var gabrielBankId: UUID
        lateinit var joaoId: UUID
        lateinit var joaoBankId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                gabrielId = user(email = "gabriel@example.com", firstName = "Gabriel", lastName = "Silva", defaultCurrency = "BRL")
                gabrielBankId = bankAccount(name = "Gabriel bank", balance = 1000, currency = "BRL", showOnDashboard = true)
                joaoId = user(email = "joao@example.com", firstName = "Joao", lastName = "Souza", defaultCurrency = "BRL")
                joaoBankId = bankAccount(name = "Joao bank", balance = 1000, currency = "BRL", showOnDashboard = true)
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = gabrielId,
                    associatedItemIds = setOf(gabrielBankId, joaoBankId),
                )
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = joaoId,
                    associatedItemIds = setOf(gabrielBankId, joaoBankId),
                )

                switchUser(gabrielId)
                expense(
                    value = 500,
                    originId = gabrielBankId,
                    groupId = groupId,
                    date = LocalDate.of(2026, 4, 5),
                    name = "Gabriel full expense",
                )

                switchUser(joaoId)
                expense(
                    value = 3,
                    originId = joaoBankId,
                    groupId = groupId,
                    date = LocalDate.of(2026, 4, 6),
                    name = "Joao full expense",
                )

                switchUser(gabrielId)
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        groupId = groupId,
                        originId = gabrielBankId,
                        date = LocalDate.of(2026, 4, 7),
                        value = BigDecimal("50.00"),
                        name = "Split executed expense",
                        confirmed = true,
                        paymentType = PaymentType.UNIQUE,
                        beneficiaries =
                            listOf(
                                NewWalletBeneficiaryLeg(userId = gabrielId, benefitPercent = BigDecimal("50.00")),
                                NewWalletBeneficiaryLeg(userId = joaoId, benefitPercent = BigDecimal("50.00")),
                            ),
                    ),
                )
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        groupId = groupId,
                        originId = gabrielBankId,
                        date = LocalDate.of(2026, 4, 20),
                        value = BigDecimal("10.00"),
                        name = "Split projected expense",
                        confirmed = true,
                        paymentType = PaymentType.UNIQUE,
                        beneficiaries =
                            listOf(
                                NewWalletBeneficiaryLeg(userId = gabrielId, benefitPercent = BigDecimal("50.00")),
                                NewWalletBeneficiaryLeg(userId = joaoId, benefitPercent = BigDecimal("50.00")),
                            ),
                    ),
                )

                fetchGroupOverview(groupId = groupId, selectedMonth = selectedMonth)
            }

            then {
                groupOverviewExpenseForMonthShouldBe(month = selectedMonth, executed = 553, projected = 10)
                groupOverviewExpenseByMemberForMonthShouldBe(
                    memberId = gabrielId,
                    month = selectedMonth,
                    executed = 525,
                    projected = 5,
                )
                groupOverviewExpenseByMemberForMonthShouldBe(
                    memberId = joaoId,
                    month = selectedMonth,
                    executed = 28,
                    projected = 5,
                )
                groupOverviewExpenseCategoryByMemberSliceShouldBe(
                    memberId = gabrielId,
                    label = "PREDEFINED_UNCATEGORIZED",
                    expected = 530,
                )
                groupOverviewExpenseCategoryByMemberSliceShouldBe(
                    memberId = joaoId,
                    label = "PREDEFINED_UNCATEGORIZED",
                    expected = 33,
                )
                groupOverviewExpenseByMemberSliceShouldBe(memberId = gabrielId, expected = 530)
                groupOverviewExpenseByMemberSliceShouldBe(memberId = joaoId, expected = 33)
            }
        }
    }

    @Test
    fun `group overview debt pairs should net opposite directions and accumulate unpaid months including future selected month`() {
        val today = LocalDate.of(2026, 4, 10)
        val selectedMonth = YearMonth.of(2026, 6)
        val groupService = ScenarioGroupService()
        val selectedGroupId = groupService.createGroup(name = "Debt Group")

        lateinit var payerId: UUID
        lateinit var payerBankId: UUID
        lateinit var receiverId: UUID
        lateinit var receiverBankId: UUID

        val groupDebtService =
            object : GroupDebtService by NoOpGroupDebtService {
                override suspend fun getWorkspace(
                    userId: UUID,
                    groupId: UUID,
                ): GroupDebtWorkspace {
                    if (groupId != selectedGroupId) {
                        return GroupDebtWorkspace(balances = emptyList())
                    }

                    return GroupDebtWorkspace(
                        balances =
                            listOf(
                                GroupDebtPairBalance(
                                    payerId = payerId,
                                    receiverId = receiverId,
                                    currency = "BRL",
                                    outstandingAmount = BigDecimal("140.00"),
                                    monthlyComposition =
                                        listOf(
                                            GroupDebtMonthlyComposition(
                                                month = YearMonth.of(2026, 1),
                                                netAmount = BigDecimal("100.00"),
                                                chargeDelta = BigDecimal("100.00"),
                                                settlementDelta = BigDecimal.ZERO,
                                                manualAdjustmentDelta = BigDecimal.ZERO,
                                            ),
                                            GroupDebtMonthlyComposition(
                                                month = YearMonth.of(2026, 2),
                                                netAmount = BigDecimal("40.00"),
                                                chargeDelta = BigDecimal("40.00"),
                                                settlementDelta = BigDecimal.ZERO,
                                                manualAdjustmentDelta = BigDecimal.ZERO,
                                            ),
                                        ),
                                ),
                                GroupDebtPairBalance(
                                    payerId = receiverId,
                                    receiverId = payerId,
                                    currency = "BRL",
                                    outstandingAmount = BigDecimal("50.00"),
                                    monthlyComposition =
                                        listOf(
                                            GroupDebtMonthlyComposition(
                                                month = YearMonth.of(2026, 1),
                                                netAmount = BigDecimal("30.00"),
                                                chargeDelta = BigDecimal("30.00"),
                                                settlementDelta = BigDecimal.ZERO,
                                                manualAdjustmentDelta = BigDecimal.ZERO,
                                            ),
                                            GroupDebtMonthlyComposition(
                                                month = YearMonth.of(2026, 3),
                                                netAmount = BigDecimal("20.00"),
                                                chargeDelta = BigDecimal("20.00"),
                                                settlementDelta = BigDecimal.ZERO,
                                                manualAdjustmentDelta = BigDecimal.ZERO,
                                            ),
                                        ),
                                ),
                            ),
                    )
                }
            }

        walletScenario(initialDate = today, groupService = groupService, groupDebtService = groupDebtService) {
            given {
                payerId = user(email = "payer@example.com", firstName = "Payer", lastName = "User", defaultCurrency = "BRL")
                payerBankId = bankAccount(name = "Payer bank", balance = 1000, currency = "BRL", showOnDashboard = true)
                receiverId = user(email = "receiver@example.com", firstName = "Receiver", lastName = "User", defaultCurrency = "BRL")
                receiverBankId = bankAccount(name = "Receiver bank", balance = 1000, currency = "BRL", showOnDashboard = true)
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = selectedGroupId,
                    userId = payerId,
                    associatedItemIds = setOf(payerBankId, receiverBankId),
                )
                groupService.upsertMemberScope(
                    groupId = selectedGroupId,
                    userId = receiverId,
                    associatedItemIds = setOf(payerBankId, receiverBankId),
                )

                switchUser(payerId)
                fetchGroupOverview(groupId = selectedGroupId, selectedMonth = selectedMonth)
            }

            then {
                groupOverviewDebtPairsSizeShouldBe(1)
                groupOverviewDebtPairShouldBe(payerId = payerId, receiverId = receiverId, outstanding = 90)
                groupOverviewDebtPairShouldNotExist(payerId = receiverId, receiverId = payerId)
                groupOverviewDebtPairDetailLabelsShouldContain(
                    payerId = payerId,
                    receiverId = receiverId,
                    expectedLabels = listOf("01-2026", "02-2026"),
                )
                groupOverviewCardShouldBe(OverviewDashboardCardKey.GROUP_MEMBER_DEBTS, 90)
            }
        }
    }

    @Test
    fun `group transactions feed should merge origins paginate by date-id and filter by group-month using transaction date`() {
        val today = LocalDate.of(2026, 4, 30)
        val selectedMonth = YearMonth.of(2026, 4)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Feed Group")
        val otherGroupId = groupService.createGroup(name = "Outside Group")

        lateinit var userAId: UUID
        lateinit var userABankId: UUID
        lateinit var userACreditCardId: UUID
        lateinit var userBId: UUID
        lateinit var userBBankId: UUID
        lateinit var aprilBankEventId: UUID
        lateinit var aprilCardEventId: UUID
        lateinit var aprilMemberBEventId: UUID
        lateinit var marchCardEventId: UUID
        lateinit var otherGroupEventId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                userAId = user(email = "feed-a@example.com", firstName = "Feed", lastName = "A", defaultCurrency = "BRL")
                userABankId = bankAccount(name = "Feed A bank", balance = 1000, currency = "BRL", showOnDashboard = false)
                userACreditCardId =
                    creditCard(
                        limit = 2000,
                        name = "Feed A credit",
                        currency = "BRL",
                        dueDay = 10,
                        daysBetweenDueAndClosing = 7,
                        dueOnNextBusinessDay = false,
                        showOnDashboard = false,
                    )
                userBId = user(email = "feed-b@example.com", firstName = "Feed", lastName = "B", defaultCurrency = "BRL")
                userBBankId = bankAccount(name = "Feed B bank", balance = 500, currency = "BRL", showOnDashboard = true)
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = userAId,
                    associatedItemIds = setOf(userABankId, userACreditCardId, userBBankId),
                )
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = userBId,
                    associatedItemIds = setOf(userABankId, userACreditCardId, userBBankId),
                )
                groupService.upsertMemberScope(
                    groupId = otherGroupId,
                    userId = userAId,
                    associatedItemIds = setOf(userABankId),
                )

                switchUser(userAId)
                expense(
                    value = 12,
                    originId = userACreditCardId,
                    groupId = groupId,
                    date = LocalDate.of(2026, 3, 31),
                    billDate = LocalDate.of(2026, 4, 5),
                    name = "March card expense",
                )
                marchCardEventId = lastWalletEventId()

                expense(
                    value = 30,
                    originId = userABankId,
                    groupId = groupId,
                    date = LocalDate.of(2026, 4, 10),
                    name = "April bank expense",
                )
                aprilBankEventId = lastWalletEventId()

                expense(
                    value = 40,
                    originId = userACreditCardId,
                    groupId = groupId,
                    date = LocalDate.of(2026, 4, 12),
                    billDate = LocalDate.of(2026, 4, 5),
                    name = "April card expense",
                )
                aprilCardEventId = lastWalletEventId()

                expense(
                    value = 50,
                    originId = userABankId,
                    groupId = otherGroupId,
                    date = LocalDate.of(2026, 4, 11),
                    name = "Other group expense",
                )
                otherGroupEventId = lastWalletEventId()

                switchUser(userBId)
                expense(
                    value = 15,
                    originId = userBBankId,
                    groupId = groupId,
                    date = LocalDate.of(2026, 4, 9),
                    name = "April member B expense",
                )
                aprilMemberBEventId = lastWalletEventId()

                switchUser(userAId)
                fetchGroupFeed(
                    groupId = groupId,
                    selectedMonth = selectedMonth,
                    pageSize = 2,
                    entryTypes = setOf(WalletEntryType.EXPENSE),
                )
            }

            then {
                groupFeedShouldHaveSize(2)
                groupFeedShouldHaveNext(true)
                groupFeedShouldContainOnlyGroup(groupId)
                groupFeedShouldContainOnlyTypes(setOf(WalletEntryType.EXPENSE))
                groupFeedShouldContainEventId(aprilCardEventId)
                groupFeedShouldContainEventId(aprilBankEventId)
                groupFeedShouldNotContainEventId(marchCardEventId)
                groupFeedShouldNotContainEventId(otherGroupEventId)
                groupFeedShouldBeOrderedByDateDescIdDesc()
            }

            `when` {
                fetchNextGroupFeedPage()
            }

            then {
                groupFeedShouldHaveSize(1)
                groupFeedShouldHaveNext(false)
                groupFeedShouldContainEventId(aprilMemberBEventId)
                groupFeedShouldNotContainEventId(marchCardEventId)
                groupFeedShouldNotContainEventId(otherGroupEventId)
                groupFeedShouldBeOrderedByDateDescIdDesc()
            }
        }
    }
}
