package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulationScopeType
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.repositories.GoalLedgerCommittedSummaryRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.scenarios.support.NoOpGroupDebtService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.r2dbc.core.DatabaseClient
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class PlanningSimulationEngineImplTest {
    @Test
    fun `projected cash flow should call recurrence simulation once for member set`() =
        runBlocking {
            val userIds = setOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
            val bankId = UUID.randomUUID()
            val fromDate = LocalDate.of(2026, 4, 2)
            val toDate = LocalDate.of(2026, 4, 30)

            val recurrenceService =
                FakeRecurrenceSimulationService(
                    events =
                        listOf(
                            EventListResponse(
                                id = null,
                                type = WalletEntryType.EXPENSE,
                                name = "Projected",
                                category = null,
                                user = null,
                                group = null,
                                tags = emptyList(),
                                observations = null,
                                date = LocalDate.of(2026, 4, 10),
                                confirmed = false,
                                installment = null,
                                recurrenceConfigId = null,
                                recurrenceConfig = null,
                                currency = "BRL",
                                entries =
                                    listOf(
                                        EventListResponse.EntryResponse(
                                            value = BigDecimal("-10.00"),
                                            walletItem =
                                                com.ynixt.sharedfinances.domain.models.bankaccount
                                                    .BankAccount(
                                                        name = "Main",
                                                        enabled = true,
                                                        userId = userIds.first(),
                                                        currency = "BRL",
                                                        balance = BigDecimal.ZERO,
                                                    ).also { it.id = bankId },
                                            walletItemId = bankId,
                                            billDate = null,
                                            billId = null,
                                        ),
                                    ),
                            ),
                        ),
                )

            val engine =
                PlanningSimulationEngineImpl(
                    walletItemRepository = mock(WalletItemRepository::class.java),
                    groupWalletItemRepository = mock(GroupWalletItemRepository::class.java),
                    groupUsersRepository = mock(GroupUsersRepository::class.java),
                    recurrenceSimulationService = recurrenceService,
                    goalLedgerSummaryRepository = mock(GoalLedgerCommittedSummaryRepository::class.java),
                    groupDebtService = NoOpGroupDebtService,
                    dbClient = mock(DatabaseClient::class.java),
                    clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC),
                )

            val scopeDataClass = PlanningSimulationEngineImpl::class.nestedClasses.first { it.simpleName == "ScopeData" }
            val scopeDataCtor = scopeDataClass.primaryConstructor!!.also { it.isAccessible = true }
            val scopeData =
                scopeDataCtor.call(
                    PlanningSimulationScopeType.USER,
                    listOf(
                        WalletItemEntity(
                            type = WalletItemType.BANK_ACCOUNT,
                            name = "Main",
                            enabled = true,
                            userId = userIds.first(),
                            currency = "BRL",
                            balance = BigDecimal.ZERO,
                            totalLimit = null,
                            dueDay = null,
                            daysBetweenDueAndClosing = null,
                            dueOnNextBusinessDay = null,
                            showOnDashboard = true,
                        ).also { it.id = bankId },
                    ),
                    emptyList<WalletItemEntity>(),
                    userIds,
                    null,
                    null,
                )

            val method = PlanningSimulationEngineImpl::class.declaredFunctions.first { it.name == "loadProjectedCashFlowByMonthCurrency" }
            method.isAccessible = true

            val result =
                method.callSuspend(
                    engine,
                    scopeData,
                    fromDate,
                    toDate,
                    setOf(bankId),
                ) as Map<*, *>

            assertThat(result).isNotEmpty
            assertThat(recurrenceService.batchUserCalls).isEqualTo(1)
            assertThat(recurrenceService.singleUserCalls).isEqualTo(0)
        }

    private class FakeRecurrenceSimulationService(
        private val events: List<EventListResponse>,
    ) : RecurrenceSimulationService {
        var batchUserCalls: Int = 0
        var singleUserCalls: Int = 0

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
            if (userId != null) {
                singleUserCalls += 1
            }
            return events
        }

        override suspend fun simulateGenerationForUsers(
            minimumEndExecution: LocalDate?,
            maximumNextExecution: LocalDate?,
            userIds: Set<UUID>,
            billDate: LocalDate?,
        ): List<EventListResponse> {
            batchUserCalls += 1
            return events
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
            config: RecurrenceEventEntity,
            walletItems: List<WalletItem>,
            user: UserEntity?,
            group: GroupEntity?,
            category: WalletEntryCategoryEntity?,
            simulateBillForRecurrence: Boolean,
        ): EventListResponse = events.first()
    }
}
