package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
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
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

class GroupDebtServiceImplMonthSelectionTest {
    @Test
    fun `past month should ignore projected debt movements`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 2)

            val recurrenceSimulation =
                RecordingRecurrenceSimulationService(events = listOf(projectedExpenseEvent(selectedMonth.atDay(15), payerId, receiverId)))
            val service =
                createService(
                    groupId = groupId,
                    rows =
                        listOf(
                            compositionRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = YearMonth.of(2026, 1),
                                amount = BigDecimal("100.00"),
                            ),
                            compositionRow(
                                payerId = payerId,
                                receiverId = receiverId,
                                month = YearMonth.of(2026, 3),
                                amount = BigDecimal("30.00"),
                            ),
                        ),
                    recurrenceSimulationService = recurrenceSimulation,
                )

            val workspace = service.getWorkspaceForMonth(userId = userId, groupId = groupId, selectedMonth = selectedMonth)

            assertThat(recurrenceSimulation.simulateGenerationWithFiltersCalls).isZero()
            assertThat(workspace.balances).hasSize(1)
            assertThat(workspace.balances.first().outstandingAmount).isEqualByComparingTo("100.00")
            assertThat(
                workspace.balances
                    .first()
                    .monthlyComposition
                    .map { it.month },
            ).containsExactly(YearMonth.of(2026, 1))
        }

    @Test
    fun `current month should include projected recurring and installment movements`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 4)

            val recurrenceSimulation =
                RecordingRecurrenceSimulationService(
                    events = listOf(projectedExpenseEvent(selectedMonth.atDay(20), payerId, receiverId)),
                )
            val service =
                createService(
                    groupId = groupId,
                    rows = emptyList(),
                    recurrenceSimulationService = recurrenceSimulation,
                )

            val workspace = service.getWorkspaceForMonth(userId = userId, groupId = groupId, selectedMonth = selectedMonth)

            assertThat(recurrenceSimulation.simulateGenerationWithFiltersCalls).isEqualTo(1)
            assertThat(workspace.balances).hasSize(1)
            val balance = workspace.balances.first()
            assertThat(balance.payerId).isEqualTo(receiverId)
            assertThat(balance.receiverId).isEqualTo(payerId)
            assertThat(balance.outstandingAmount).isEqualByComparingTo("20.00")
            assertThat(balance.monthlyComposition).hasSize(1)
            assertThat(balance.monthlyComposition.first().month).isEqualTo(selectedMonth)
            assertThat(balance.monthlyComposition.first().chargeDelta).isEqualByComparingTo("20.00")
            assertThat(balance.monthlyComposition.first().netAmount).isEqualByComparingTo("20.00")
        }

    @Test
    fun `future month should combine persisted values until selected month with selected month projected movement`() =
        runBlocking {
            val groupId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val selectedMonth = YearMonth.of(2026, 6)

            val recurrenceSimulation =
                RecordingRecurrenceSimulationService(
                    events =
                        listOf(
                            projectedExpenseEvent(selectedMonth.minusMonths(1).atDay(5), payerId, receiverId),
                            projectedExpenseEvent(selectedMonth.atDay(5), payerId, receiverId),
                        ),
                )
            val service =
                createService(
                    groupId = groupId,
                    rows =
                        listOf(
                            compositionRow(
                                payerId = receiverId,
                                receiverId = payerId,
                                month = YearMonth.of(2026, 5),
                                amount = BigDecimal("30.00"),
                            ),
                        ),
                    recurrenceSimulationService = recurrenceSimulation,
                )

            val workspace = service.getWorkspaceForMonth(userId = userId, groupId = groupId, selectedMonth = selectedMonth)

            assertThat(recurrenceSimulation.simulateGenerationWithFiltersCalls).isEqualTo(1)
            assertThat(workspace.balances).hasSize(1)
            val balance = workspace.balances.first()
            assertThat(balance.payerId).isEqualTo(receiverId)
            assertThat(balance.receiverId).isEqualTo(payerId)
            assertThat(balance.outstandingAmount).isEqualByComparingTo("50.00")
            assertThat(balance.monthlyComposition.map { it.month })
                .containsExactly(YearMonth.of(2026, 5), selectedMonth)
            assertThat(balance.monthlyComposition.map { it.netAmount })
                .containsExactly(BigDecimal("30.00"), BigDecimal("20.00"))
        }

    private fun createService(
        groupId: UUID,
        rows: List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow>,
        recurrenceSimulationService: RecordingRecurrenceSimulationService,
    ): GroupDebtServiceImpl {
        val debtDatabaseClientRepository = Mockito.mock(GroupMemberDebtDatabaseClientRepository::class.java)
        Mockito
            .`when`(debtDatabaseClientRepository.listMonthlyComposition(groupId))
            .thenReturn(Flux.fromIterable(rows))

        return GroupDebtServiceImpl(
            groupPermissionService = AllowAllGroupPermissionService,
            movementRepository = Mockito.mock(GroupMemberDebtMovementSpringDataRepository::class.java),
            debtDatabaseClientRepository = debtDatabaseClientRepository,
            walletEventRepository = Mockito.mock(WalletEventRepository::class.java),
            walletEventListService = Mockito.mock(WalletEventListService::class.java),
            recurrenceSimulationService = recurrenceSimulationService,
            clock = fixedClock(),
        )
    }

    private fun compositionRow(
        payerId: UUID,
        receiverId: UUID,
        month: YearMonth,
        amount: BigDecimal,
    ) = GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow(
        payerId = payerId,
        receiverId = receiverId,
        currency = "BRL",
        month = month,
        netAmount = amount,
        chargeDelta = amount,
        settlementDelta = BigDecimal.ZERO,
        manualAdjustmentDelta = BigDecimal.ZERO,
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
        var simulateGenerationWithFiltersCalls: Int = 0

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
        ): List<EventListResponse> {
            simulateGenerationWithFiltersCalls += 1
            return events
        }

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
