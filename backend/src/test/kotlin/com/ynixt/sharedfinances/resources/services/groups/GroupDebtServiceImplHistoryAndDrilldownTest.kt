package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtHistoryFilter
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupMemberDebtDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupMemberDebtMovementSpringDataRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

class GroupDebtServiceImplHistoryAndDrilldownTest {
    @Test
    fun `history should filter by debt month and append selected-month projected rows`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)

            val service =
                createService(
                    groupId = groupId,
                    compositionRows = emptyList(),
                    historyRows =
                        listOf(
                            historyRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = selectedMonth,
                                deltaSigned = BigDecimal("12.00"),
                                reasonKind = GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT,
                            ),
                        ),
                    projectedEvents =
                        listOf(
                            projectedExpenseEvent(date = selectedMonth.atDay(20), payerId = payerId, receiverId = receiverId),
                            projectedExpenseEvent(date = selectedMonth.plusMonths(1).atDay(5), payerId = payerId, receiverId = receiverId),
                        ),
                )

            val history =
                service.listHistory(
                    userId = userId,
                    groupId = groupId,
                    filter =
                        GroupDebtHistoryFilter(
                            payerId = payerId,
                            receiverId = receiverId,
                            currency = "BRL",
                            selectedMonth = selectedMonth,
                        ),
                )

            assertThat(history).hasSize(2)
            assertThat(history.map { it.month }).containsOnly(selectedMonth)
            assertThat(history.count { it.projected }).isEqualTo(1)
            assertThat(history.first { it.projected }.transactionDate).isEqualTo(selectedMonth.atDay(20))
        }

    @Test
    fun `monthly drilldown should mix executed manual settlement and projected contributors`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)

            val service =
                createService(
                    groupId = groupId,
                    compositionRows =
                        listOf(
                            compositionRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = selectedMonth,
                                netAmount = BigDecimal("15.00"),
                                chargeDelta = BigDecimal("50.00"),
                                settlementDelta = BigDecimal("-30.00"),
                                manualAdjustmentDelta = BigDecimal("-5.00"),
                            ),
                        ),
                    historyRows =
                        listOf(
                            historyRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = selectedMonth,
                                deltaSigned = BigDecimal("50.00"),
                                reasonKind = GroupDebtMovementReasonKind.BENEFICIARY_CHARGE,
                            ),
                            historyRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = selectedMonth,
                                deltaSigned = BigDecimal("-30.00"),
                                reasonKind = GroupDebtMovementReasonKind.DEBT_SETTLEMENT,
                            ),
                            historyRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = selectedMonth,
                                deltaSigned = BigDecimal("-5.00"),
                                reasonKind = GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT,
                            ),
                        ),
                    projectedEvents =
                        listOf(
                            projectedExpenseEvent(date = selectedMonth.atDay(20), payerId = payerId, receiverId = receiverId),
                        ),
                )

            val drilldown =
                service.getMonthlyDrilldown(
                    userId = userId,
                    groupId = groupId,
                    payerId = payerId,
                    receiverId = receiverId,
                    currency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertThat(drilldown.netAmount).isEqualByComparingTo("35.00")
            assertThat(drilldown.chargeDelta).isEqualByComparingTo("70.00")
            assertThat(drilldown.settlementDelta).isEqualByComparingTo("-30.00")
            assertThat(drilldown.manualAdjustmentDelta).isEqualByComparingTo("-5.00")
            assertThat(drilldown.lines).hasSize(4)
            assertThat(drilldown.lines.count { it.projected }).isEqualTo(1)
            assertThat(drilldown.lines.map { it.reasonKind })
                .contains(
                    GroupDebtMovementReasonKind.BENEFICIARY_CHARGE,
                    GroupDebtMovementReasonKind.DEBT_SETTLEMENT,
                    GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT,
                )
        }

    @Test
    fun `history and drilldown should include projected credit card installments by bill month`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 6)
            val projectedDate = selectedMonth.minusMonths(1).atDay(25)

            val service =
                createService(
                    groupId = groupId,
                    compositionRows = emptyList(),
                    historyRows = emptyList(),
                    projectedEvents =
                        listOf(
                            projectedCreditCardExpenseEvent(
                                date = projectedDate,
                                billDate = selectedMonth.atDay(1),
                                payerId = payerId,
                                receiverId = receiverId,
                            ),
                        ),
                )

            val history =
                service.listHistory(
                    userId = userId,
                    groupId = groupId,
                    filter =
                        GroupDebtHistoryFilter(
                            payerId = receiverId,
                            receiverId = payerId,
                            currency = "BRL",
                            selectedMonth = selectedMonth,
                        ),
                )
            val drilldown =
                service.getMonthlyDrilldown(
                    userId = userId,
                    groupId = groupId,
                    payerId = receiverId,
                    receiverId = payerId,
                    currency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertThat(history).hasSize(1)
            assertThat(history.single().projected).isTrue()
            assertThat(history.single().month).isEqualTo(selectedMonth)
            assertThat(history.single().transactionDate).isEqualTo(projectedDate)
            assertThat(drilldown.netAmount).isEqualByComparingTo("20.00")
            assertThat(drilldown.chargeDelta).isEqualByComparingTo("20.00")
            assertThat(drilldown.lines).hasSize(1)
            assertThat(drilldown.lines.single().transactionDate).isEqualTo(projectedDate)
            assertThat(drilldown.lines.single().month).isEqualTo(selectedMonth)
        }

    @Test
    fun `monthly drilldown should include carried over unpaid balances from previous months`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 6)

            val service =
                createService(
                    groupId = groupId,
                    compositionRows =
                        listOf(
                            compositionRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = selectedMonth.minusMonths(1),
                                netAmount = BigDecimal("538.86"),
                                chargeDelta = BigDecimal("538.86"),
                            ),
                        ),
                    historyRows = emptyList(),
                    projectedEvents = emptyList(),
                )

            val drilldown =
                service.getMonthlyDrilldown(
                    userId = userId,
                    groupId = groupId,
                    payerId = payerId,
                    receiverId = receiverId,
                    currency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertThat(drilldown.netAmount).isEqualByComparingTo("538.86")
            assertThat(drilldown.lines).hasSize(1)
            assertThat(drilldown.lines.single().carriedOver).isTrue()
            assertThat(drilldown.lines.single().projected).isFalse()
            assertThat(drilldown.lines.single().month).isEqualTo(selectedMonth.minusMonths(1))
            assertThat(drilldown.lines.single().deltaSigned).isEqualByComparingTo("538.86")
        }

    @Test
    fun `monthly drilldown should group carryover debts from the same previous month into a single line`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 6)
            val previousMonth = selectedMonth.minusMonths(1)

            val service =
                createService(
                    groupId = groupId,
                    compositionRows =
                        listOf(
                            compositionRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = previousMonth,
                                netAmount = BigDecimal("100.00"),
                                chargeDelta = BigDecimal("100.00"),
                            ),
                            compositionRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = previousMonth,
                                netAmount = BigDecimal("38.86"),
                                chargeDelta = BigDecimal("38.86"),
                            ),
                        ),
                    historyRows = emptyList(),
                    projectedEvents = emptyList(),
                )

            val drilldown =
                service.getMonthlyDrilldown(
                    userId = userId,
                    groupId = groupId,
                    payerId = payerId,
                    receiverId = receiverId,
                    currency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertThat(drilldown.netAmount).isEqualByComparingTo("138.86")
            assertThat(drilldown.lines).hasSize(1)
            assertThat(drilldown.lines.single().carriedOver).isTrue()
            assertThat(drilldown.lines.single().month).isEqualTo(previousMonth)
            assertThat(drilldown.lines.single().deltaSigned).isEqualByComparingTo("138.86")
        }

    @Test
    fun `monthly drilldown should include projected carryover from intermediate future months`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 8)

            val service =
                createService(
                    groupId = groupId,
                    compositionRows =
                        listOf(
                            compositionRow(
                                payerId = receiverId,
                                receiverId = payerId,
                                month = YearMonth.of(2026, 5),
                                netAmount = BigDecimal("538.86"),
                                chargeDelta = BigDecimal("538.86"),
                            ),
                        ),
                    historyRows = emptyList(),
                    projectedEvents =
                        listOf(
                            projectedExpenseEvent(date = selectedMonth.minusMonths(2).atDay(5), payerId = payerId, receiverId = receiverId),
                            projectedExpenseEvent(date = selectedMonth.atDay(5), payerId = payerId, receiverId = receiverId),
                        ),
                )

            val drilldown =
                service.getMonthlyDrilldown(
                    userId = userId,
                    groupId = groupId,
                    payerId = receiverId,
                    receiverId = payerId,
                    currency = "BRL",
                    selectedMonth = selectedMonth,
                )

            assertThat(drilldown.netAmount).isEqualByComparingTo("578.86")
            assertThat(drilldown.chargeDelta).isEqualByComparingTo("20.00")
            assertThat(drilldown.lines).hasSize(3)
            assertThat(drilldown.lines.count { it.carriedOver }).isEqualTo(2)
            assertThat(
                drilldown.lines
                    .filter { it.carriedOver }
                    .map { it.month },
            ).containsExactly(YearMonth.of(2026, 5), YearMonth.of(2026, 6))
            assertThat(
                drilldown.lines
                    .first { it.month == YearMonth.of(2026, 6) },
            ).matches { it.carriedOver && it.projected && it.deltaSigned.compareTo(BigDecimal("20.00")) == 0 }
            assertThat(
                drilldown.lines
                    .first { it.month == selectedMonth },
            ).matches { !it.carriedOver && it.projected && it.deltaSigned.compareTo(BigDecimal("20.00")) == 0 }
        }

    private fun createService(
        groupId: UUID,
        compositionRows: List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow>,
        historyRows: List<GroupMemberDebtDatabaseClientRepository.DebtMovementHistoryRow>,
        projectedEvents: List<EventListResponse>,
    ): GroupDebtServiceImpl {
        val debtDatabaseClientRepository = Mockito.mock(GroupMemberDebtDatabaseClientRepository::class.java)
        Mockito
            .`when`(debtDatabaseClientRepository.listMonthlyComposition(groupId))
            .thenReturn(Flux.fromIterable(compositionRows))
        Mockito
            .`when`(
                debtDatabaseClientRepository.listHistory(
                    Mockito.eq(groupId),
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.any(),
                ),
            ).thenReturn(Flux.fromIterable(historyRows))

        return GroupDebtServiceImpl(
            groupPermissionService = AllowAllGroupPermissionService,
            movementRepository = Mockito.mock(GroupMemberDebtMovementSpringDataRepository::class.java),
            debtDatabaseClientRepository = debtDatabaseClientRepository,
            walletEventRepository = Mockito.mock(WalletEventRepository::class.java),
            walletEventListService = Mockito.mock(WalletEventListService::class.java),
            recurrenceSimulationService = RecordingRecurrenceSimulationService(projectedEvents),
            clock = fixedClock(),
        )
    }

    private fun compositionRow(
        payerId: UUID,
        receiverId: UUID,
        month: YearMonth,
        netAmount: BigDecimal,
        chargeDelta: BigDecimal = netAmount,
        settlementDelta: BigDecimal = BigDecimal.ZERO,
        manualAdjustmentDelta: BigDecimal = BigDecimal.ZERO,
    ) = GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow(
        payerId = payerId,
        receiverId = receiverId,
        currency = "BRL",
        month = month,
        netAmount = netAmount,
        chargeDelta = chargeDelta,
        settlementDelta = settlementDelta,
        manualAdjustmentDelta = manualAdjustmentDelta,
    )

    private fun historyRow(
        payerId: UUID,
        receiverId: UUID,
        month: YearMonth,
        deltaSigned: BigDecimal,
        reasonKind: GroupDebtMovementReasonKind,
    ) = GroupMemberDebtDatabaseClientRepository.DebtMovementHistoryRow(
        id = UUID.randomUUID(),
        payerId = payerId,
        receiverId = receiverId,
        month = month,
        currency = "BRL",
        deltaSigned = deltaSigned,
        reasonKind = reasonKind.name,
        createdByUserId = payerId,
        note = null,
        sourceWalletEventId = null,
        sourceMovementId = null,
        createdAt = OffsetDateTime.of(month.atDay(10), java.time.LocalTime.NOON, ZoneOffset.UTC),
    )

    private fun projectedExpenseEvent(
        date: LocalDate,
        payerId: UUID,
        receiverId: UUID,
    ): EventListResponse {
        val payerWallet = wallet(ownerId = payerId)

        return EventListResponse(
            id = null,
            type = WalletEntryType.EXPENSE,
            name = "Projected split expense",
            category = null,
            user = null,
            group = null,
            tags = null,
            observations = null,
            date = date,
            confirmed = true,
            installment = null,
            recurrenceConfigId = null,
            recurrenceConfig = null,
            currency = "BRL",
            beneficiaries =
                listOf(
                    EventListResponse.BeneficiaryResponse(userId = payerId, benefitPercent = BigDecimal("50.00")),
                    EventListResponse.BeneficiaryResponse(userId = receiverId, benefitPercent = BigDecimal("50.00")),
                ),
            entries =
                listOf(
                    EventListResponse.EntryResponse(
                        value = BigDecimal("-40.00"),
                        walletItem = payerWallet,
                        walletItemId = requireNotNull(payerWallet.id),
                        billDate = null,
                        billId = null,
                    ),
                ),
        )
    }

    private fun projectedCreditCardExpenseEvent(
        date: LocalDate,
        billDate: LocalDate,
        payerId: UUID,
        receiverId: UUID,
    ): EventListResponse {
        val payerWallet = creditCard(ownerId = payerId)

        return EventListResponse(
            id = null,
            type = WalletEntryType.EXPENSE,
            name = "Projected installment expense",
            category = null,
            user = null,
            group = null,
            tags = null,
            observations = null,
            date = date,
            confirmed = true,
            installment = 2,
            recurrenceConfigId = null,
            recurrenceConfig = null,
            currency = "BRL",
            beneficiaries =
                listOf(
                    EventListResponse.BeneficiaryResponse(userId = payerId, benefitPercent = BigDecimal("50.00")),
                    EventListResponse.BeneficiaryResponse(userId = receiverId, benefitPercent = BigDecimal("50.00")),
                ),
            entries =
                listOf(
                    EventListResponse.EntryResponse(
                        value = BigDecimal("-40.00"),
                        walletItem = payerWallet,
                        walletItemId = requireNotNull(payerWallet.id),
                        billDate = billDate,
                        billId = null,
                    ),
                ),
        )
    }

    private fun wallet(ownerId: UUID): WalletItem =
        object : WalletItem(
            name = "Wallet",
            enabled = true,
            userId = ownerId,
            currency = "BRL",
            balance = BigDecimal.ZERO,
            showOnDashboard = true,
        ) {
            override val type = com.ynixt.sharedfinances.domain.enums.WalletItemType.BANK_ACCOUNT
        }.also { wallet ->
            wallet.id = UUID.randomUUID()
        }

    private fun creditCard(ownerId: UUID): WalletItem =
        object : WalletItem(
            name = "Credit card",
            enabled = true,
            userId = ownerId,
            currency = "BRL",
            balance = BigDecimal.ZERO,
            showOnDashboard = true,
        ) {
            override val type = WalletItemType.CREDIT_CARD
        }.also { wallet ->
            wallet.id = UUID.randomUUID()
        }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC)

    private object AllowAllGroupPermissionService : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ): Boolean = true

        override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> = emptySet()
    }

    private class RecordingRecurrenceSimulationService(
        private val events: List<EventListResponse>,
    ) : RecurrenceSimulationService {
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
        ): List<EventListResponse> = events

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
        ): List<EventListResponse> = events

        override suspend fun simulateGenerationForUsers(
            minimumEndExecution: LocalDate?,
            maximumNextExecution: LocalDate?,
            userIds: Set<UUID>,
            billDate: LocalDate?,
        ): List<EventListResponse> = events

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
        ): List<EventListResponse> = events

        override suspend fun simulateGenerationForCreditCard(
            bill: CreditCardBill,
            userId: UUID,
            groupIds: Set<UUID>,
            userIds: Set<UUID>,
            walletItemId: UUID?,
        ): List<EventListResponse> = events

        override suspend fun simulateGeneration(
            config: RecurrenceEventEntity,
            walletItems: List<WalletItem>,
            user: UserEntity?,
            group: GroupEntity?,
            category: WalletEntryCategoryEntity?,
            simulateBillForRecurrence: Boolean,
        ): EventListResponse = events.firstOrNull() ?: error("No simulated events configured for test")
    }
}
