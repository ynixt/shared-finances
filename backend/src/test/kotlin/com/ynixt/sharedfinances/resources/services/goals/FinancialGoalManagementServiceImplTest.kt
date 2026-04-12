package com.ynixt.sharedfinances.resources.services.goals

import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalContributionScheduleEntity
import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalEntity
import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalLedgerMovementEntity
import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalTargetEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.enums.GoalLedgerMovementKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.FinancialGoalForbiddenException
import com.ynixt.sharedfinances.domain.exceptions.http.FinancialGoalUnsupportedCurrencyException
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByGoalRow
import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByWalletRow
import com.ynixt.sharedfinances.domain.repositories.GoalCurrencyCommittedRow
import com.ynixt.sharedfinances.domain.repositories.GoalLedgerCommittedSummaryRepository
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalContributionScheduleSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalLedgerMovementSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalTargetSpringDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class FinancialGoalManagementServiceImplTest {
    @Test
    fun `listIndividualGoals should return paged user goals`() =
        runBlocking {
            val fixture = Fixture()
            val userId = UUID.randomUUID()
            val pageable = PageRequest.of(0, 2, Sort.by("name"))
            val firstGoal = goalEntity(userId = userId, name = "Emergency")
            val secondGoal = goalEntity(userId = userId, name = "Travel")

            Mockito
                .`when`(fixture.financialGoalRepository.countByUserId(userId))
                .thenReturn(Mono.just(2L))
            Mockito
                .`when`(fixture.financialGoalRepository.findAllByUserIdOrderByNameAscIdAsc(userId, pageable))
                .thenReturn(Flux.just(firstGoal, secondGoal))

            val page = fixture.service.listIndividualGoals(userId, pageable)

            assertThat(page.totalElements).isEqualTo(2L)
            assertThat(page.content.map { it.id }).containsExactly(firstGoal.id, secondGoal.id)
            assertThat(page.content.map { it.ownerUserId }).containsOnly(userId)
        }

    @Test
    fun `listGroupGoals should reject user without permission`() {
        val fixture = Fixture(groupPermissionAllowed = false)
        val userId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        assertThatThrownBy {
            runBlocking {
                fixture.service.listGroupGoals(userId, groupId, PageRequest.of(0, 10))
            }
        }.isInstanceOf(FinancialGoalForbiddenException::class.java)
    }

    @Test
    fun `listGroupGoals should return paged group goals when permission is granted`() =
        runBlocking {
            val fixture = Fixture(groupPermissionAllowed = true)
            val userId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val pageable = PageRequest.of(0, 10)
            val groupGoal = goalEntity(userId = null, groupId = groupId, name = "Team Reserve")

            Mockito
                .`when`(fixture.financialGoalRepository.countByGroupId(groupId))
                .thenReturn(Mono.just(1L))
            Mockito
                .`when`(fixture.financialGoalRepository.findAllByGroupIdOrderByNameAscIdAsc(groupId, pageable))
                .thenReturn(Flux.just(groupGoal))

            val page = fixture.service.listGroupGoals(userId, groupId, pageable)

            assertThat(page.totalElements).isEqualTo(1L)
            assertThat(page.content).singleElement().satisfies({ header ->
                assertThat(header.id).isEqualTo(groupGoal.id)
                assertThat(header.groupId).isEqualTo(groupId)
                assertThat(header.ownerUserId).isNull()
            })
        }

    @Test
    fun `listLedgerMovements should return paged movements with wallet metadata`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val goal = goalEntity(userId = userId)
            val walletId = UUID.randomUUID()
            val movement = movementEntity(goalId = goal.id!!, walletItemId = walletId, signedAmount = BigDecimal("15.20"))
            val walletModel = bankAccountModel(id = walletId, userId = userId, currency = "BRL", name = "Main account")
            val fixture = Fixture(wallets = mapOf(walletId to walletModel))
            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "movementDate"))

            fixture.stubGoal(goal)
            Mockito
                .`when`(fixture.ledgerMovementRepository.countByFinancialGoalId(goal.id!!))
                .thenReturn(Mono.just(1L))
            Mockito
                .`when`(
                    fixture.ledgerMovementRepository.findAllByFinancialGoalIdOrderByMovementDateDescIdDesc(goal.id!!, pageable),
                ).thenReturn(Flux.just(movement))

            val page = fixture.service.listLedgerMovements(userId = userId, goalId = goal.id!!, pageable = pageable)

            assertThat(page.totalElements).isEqualTo(1L)
            assertThat(page.content).singleElement().satisfies({ line ->
                assertThat(line.id).isEqualTo(movement.id)
                assertThat(line.walletItemName).isEqualTo("Main account")
                assertThat(line.currency).isEqualTo("BRL")
                assertThat(line.signedAmount).isEqualByComparingTo("15.20")
            })
        }

    @Test
    fun `listSchedules should return paged schedules with wallet metadata`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val goal = goalEntity(userId = userId)
            val walletId = UUID.randomUUID()
            val schedule = scheduleEntity(goalId = goal.id!!, walletItemId = walletId)
            val walletModel = bankAccountModel(id = walletId, userId = userId, currency = "BRL", name = "Main account")
            val fixture = Fixture(wallets = mapOf(walletId to walletModel))
            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "nextExecution"))

            fixture.stubGoal(goal)
            Mockito
                .`when`(fixture.scheduleRepository.countByFinancialGoalId(goal.id!!))
                .thenReturn(Mono.just(1L))
            Mockito
                .`when`(
                    fixture.scheduleRepository.findAllByFinancialGoalIdOrderByNextExecutionAscIdAsc(goal.id!!, pageable),
                ).thenReturn(Flux.just(schedule))

            val page = fixture.service.listSchedules(userId = userId, goalId = goal.id!!, pageable = pageable)

            assertThat(page.totalElements).isEqualTo(1L)
            assertThat(page.content).singleElement().satisfies({ line ->
                assertThat(line.id).isEqualTo(schedule.id)
                assertThat(line.walletItemName).isEqualTo("Main account")
                assertThat(line.currency).isEqualTo("BRL")
                assertThat(line.amount).isEqualByComparingTo("20.00")
            })
        }

    @Test
    fun `allocateImmediate should reject wallet currency not targeted by goal`() {
        val userId = UUID.randomUUID()
        val goal = goalEntity(userId = userId)
        val wallet = bankWalletEntity(id = UUID.randomUUID(), userId = userId, currency = "USD")
        val fixture = Fixture()

        fixture.stubGoal(goal)
        Mockito.`when`(fixture.walletItemRepository.findOneById(wallet.id!!)).thenReturn(Mono.just(wallet))
        Mockito
            .`when`(fixture.financialGoalTargetRepository.findAllByFinancialGoalId(goal.id!!))
            .thenReturn(Flux.just(targetEntity(goal.id!!, "BRL")))

        assertThatThrownBy {
            runBlocking {
                fixture.service.allocateImmediate(
                    userId = userId,
                    goalId = goal.id!!,
                    walletItemId = wallet.id!!,
                    amount = BigDecimal("10.00"),
                    allocationDate = LocalDate.of(2026, 4, 10),
                    note = null,
                )
            }
        }.isInstanceOf(FinancialGoalUnsupportedCurrencyException::class.java)
    }

    @Test
    fun `reverseAllocation should reject wallet currency not targeted by goal`() {
        val userId = UUID.randomUUID()
        val goal = goalEntity(userId = userId)
        val wallet = bankWalletEntity(id = UUID.randomUUID(), userId = userId, currency = "USD")
        val fixture = Fixture()

        fixture.stubGoal(goal)
        Mockito.`when`(fixture.walletItemRepository.findOneById(wallet.id!!)).thenReturn(Mono.just(wallet))
        Mockito
            .`when`(fixture.financialGoalTargetRepository.findAllByFinancialGoalId(goal.id!!))
            .thenReturn(Flux.just(targetEntity(goal.id!!, "BRL")))

        assertThatThrownBy {
            runBlocking {
                fixture.service.reverseAllocation(
                    userId = userId,
                    goalId = goal.id!!,
                    walletItemId = wallet.id!!,
                    amount = BigDecimal("8.00"),
                    note = "manual correction",
                )
            }
        }.isInstanceOf(FinancialGoalUnsupportedCurrencyException::class.java)
    }

    @Test
    fun `createSchedule should reject wallet currency not targeted by goal`() {
        val userId = UUID.randomUUID()
        val goal = goalEntity(userId = userId)
        val wallet = bankWalletEntity(id = UUID.randomUUID(), userId = userId, currency = "USD")
        val fixture = Fixture()

        fixture.stubGoal(goal)
        Mockito.`when`(fixture.walletItemRepository.findOneById(wallet.id!!)).thenReturn(Mono.just(wallet))
        Mockito
            .`when`(fixture.financialGoalTargetRepository.findAllByFinancialGoalId(goal.id!!))
            .thenReturn(Flux.just(targetEntity(goal.id!!, "BRL")))

        assertThatThrownBy {
            runBlocking {
                fixture.service.createSchedule(
                    userId = userId,
                    goalId = goal.id!!,
                    walletItemId = wallet.id!!,
                    amount = BigDecimal("12.00"),
                    periodicity = RecurrenceType.MONTHLY,
                    firstExecution = LocalDate.of(2026, 5, 1),
                    qtyLimit = 6,
                    removesAllocation = false,
                )
            }
        }.isInstanceOf(FinancialGoalUnsupportedCurrencyException::class.java)
    }

    @Test
    fun `updateSchedule should reject wallet currency not targeted by goal`() {
        val userId = UUID.randomUUID()
        val goal = goalEntity(userId = userId)
        val wallet = bankWalletEntity(id = UUID.randomUUID(), userId = userId, currency = "USD")
        val existingSchedule = scheduleEntity(goalId = goal.id!!, walletItemId = wallet.id!!)
        val fixture = Fixture()

        fixture.stubGoal(goal)
        Mockito
            .`when`(fixture.scheduleRepository.findByIdAndFinancialGoalId(existingSchedule.id!!, goal.id!!))
            .thenReturn(Mono.just(existingSchedule))
        Mockito.`when`(fixture.walletItemRepository.findOneById(wallet.id!!)).thenReturn(Mono.just(wallet))
        Mockito
            .`when`(fixture.financialGoalTargetRepository.findAllByFinancialGoalId(goal.id!!))
            .thenReturn(Flux.just(targetEntity(goal.id!!, "BRL")))

        assertThatThrownBy {
            runBlocking {
                fixture.service.updateSchedule(
                    userId = userId,
                    goalId = goal.id!!,
                    scheduleId = existingSchedule.id!!,
                    walletItemId = wallet.id!!,
                    amount = BigDecimal("12.00"),
                    periodicity = RecurrenceType.MONTHLY,
                    nextExecution = LocalDate.of(2026, 5, 1),
                    qtyLimit = 6,
                    removesAllocation = false,
                )
            }
        }.isInstanceOf(FinancialGoalUnsupportedCurrencyException::class.java)
    }

    @Test
    fun `deleteLedgerMovement should remove persisted movement`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val goal = goalEntity(userId = userId)
            val movement = movementEntity(goalId = goal.id!!, walletItemId = UUID.randomUUID(), signedAmount = BigDecimal("11.00"))
            val fixture = Fixture()

            fixture.stubGoal(goal)
            Mockito
                .`when`(fixture.ledgerMovementRepository.findByIdAndFinancialGoalId(movement.id!!, goal.id!!))
                .thenReturn(Mono.just(movement))
            Mockito
                .`when`(fixture.ledgerMovementRepository.deleteById(movement.id!!.toString()))
                .thenReturn(Mono.empty())

            fixture.service.deleteLedgerMovement(
                userId = userId,
                goalId = goal.id!!,
                movementId = movement.id!!,
            )

            Mockito.verify(fixture.ledgerMovementRepository).deleteById(movement.id!!.toString())
        }

    private class Fixture(
        groupPermissionAllowed: Boolean = true,
        wallets: Map<UUID, WalletItem> = emptyMap(),
    ) {
        val financialGoalRepository: FinancialGoalSpringDataRepository = Mockito.mock(FinancialGoalSpringDataRepository::class.java)
        val financialGoalTargetRepository: FinancialGoalTargetSpringDataRepository =
            Mockito.mock(
                FinancialGoalTargetSpringDataRepository::class.java,
            )
        val ledgerMovementRepository: FinancialGoalLedgerMovementSpringDataRepository =
            Mockito.mock(FinancialGoalLedgerMovementSpringDataRepository::class.java)
        val scheduleRepository: FinancialGoalContributionScheduleSpringDataRepository =
            Mockito.mock(FinancialGoalContributionScheduleSpringDataRepository::class.java)
        val walletItemRepository: WalletItemRepository = Mockito.mock(WalletItemRepository::class.java)
        private val goalLedgerSummaryRepository: GoalLedgerCommittedSummaryRepository = NoOpGoalLedgerSummaryRepository()
        private val walletItemService: WalletItemService = FakeWalletItemService(wallets)
        private val groupWalletItemRepository: GroupWalletItemRepository = Mockito.mock(GroupWalletItemRepository::class.java)
        private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
        private val groupPermissionService: GroupPermissionService = FakeGroupPermissionService(groupPermissionAllowed)
        private val recurrenceService: RecurrenceService = FakeRecurrenceService()
        private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-04-10T12:00:00Z"), ZoneOffset.UTC)

        val service =
            FinancialGoalManagementServiceImpl(
                financialGoalRepository = financialGoalRepository,
                financialGoalTargetRepository = financialGoalTargetRepository,
                ledgerMovementRepository = ledgerMovementRepository,
                scheduleRepository = scheduleRepository,
                ledgerSummaryRepository = goalLedgerSummaryRepository,
                walletItemRepository = walletItemRepository,
                walletItemService = walletItemService,
                groupWalletItemRepository = groupWalletItemRepository,
                groupRepository = groupRepository,
                groupPermissionService = groupPermissionService,
                recurrenceService = recurrenceService,
                clock = fixedClock,
            )

        fun stubGoal(goal: FinancialGoalEntity) {
            Mockito.`when`(financialGoalRepository.findById(goal.id!!.toString())).thenReturn(Mono.just(goal))
        }
    }

    private class FakeWalletItemService(
        private val walletById: Map<UUID, WalletItem>,
    ) : WalletItemService {
        override suspend fun findAllItems(
            userId: UUID,
            pageable: Pageable,
            onlyBankAccounts: Boolean,
        ): Page<WalletItem> = Page.empty(pageable)

        override suspend fun findOne(id: UUID): WalletItem? = walletById[id]

        override fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletItem> =
            flow {
                ids.forEach { id -> walletById[id]?.let { emit(it) } }
            }

        override suspend fun addBalanceById(
            id: UUID,
            balance: BigDecimal,
        ): Long = 0L
    }

    private class FakeGroupPermissionService(
        private val allowed: Boolean,
    ) : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ): Boolean = allowed

        override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> = emptySet()
    }

    private class FakeRecurrenceService : RecurrenceService {
        override fun findAllByIdIn(ids: Collection<UUID>): Flow<RecurrenceEventEntity> = emptyFlow()

        override fun findAllEntryByWalletId(
            minimumEndExecution: LocalDate?,
            maximumNextExecution: LocalDate?,
            billDate: LocalDate?,
            walletItemId: UUID,
            userId: UUID?,
            groupId: UUID?,
            sort: Sort,
        ): Flow<RecurrenceEventEntity> = emptyFlow()

        override fun findAllEntryByUserId(
            minimumEndExecution: LocalDate?,
            maximumNextExecution: LocalDate?,
            userId: UUID,
            sort: Sort,
        ): Flow<RecurrenceEventEntity> = emptyFlow()

        override fun findAllEntryByGroupId(
            minimumEndExecution: LocalDate?,
            maximumNextExecution: LocalDate?,
            groupId: UUID,
            sort: Sort,
        ): Flow<RecurrenceEventEntity> = emptyFlow()

        override fun calculateNextExecution(
            lastExecution: LocalDate,
            periodicity: RecurrenceType,
            qtyExecuted: Int,
            qtyLimit: Int?,
        ): LocalDate? = lastExecution.plusMonths(1)

        override fun calculateEndDate(
            lastExecution: LocalDate,
            periodicity: RecurrenceType,
            qtyExecuted: Int,
            qtyLimit: Int?,
        ): LocalDate? = qtyLimit?.let { lastExecution.plusMonths((it - 1).toLong()) }

        override fun calculateNextDate(
            lastExecution: LocalDate,
            periodicity: RecurrenceType,
        ): LocalDate = lastExecution.plusMonths(1)

        override suspend fun queueAllPendingOfExecution(): Int = 0
    }

    private class NoOpGoalLedgerSummaryRepository : GoalLedgerCommittedSummaryRepository {
        override fun summarizeCommittedByUserGoals(userId: UUID): Flux<GoalCommittedByWalletRow> = Flux.empty()

        override fun summarizeCommittedByUserGoalsDetailed(userId: UUID): Flux<GoalCommittedByGoalRow> = Flux.empty()

        override fun summarizeCommittedByGoal(goalId: UUID): Flux<GoalCurrencyCommittedRow> = Flux.empty()
    }

    private fun goalEntity(
        userId: UUID? = UUID.randomUUID(),
        groupId: UUID? = null,
        name: String = "Goal",
    ): FinancialGoalEntity =
        FinancialGoalEntity(
            name = name,
            description = null,
            userId = userId,
            groupId = groupId,
            deadline = null,
        ).also {
            it.id = UUID.randomUUID()
            it.createdAt = OffsetDateTime.parse("2026-04-10T12:00:00Z")
            it.updatedAt = it.createdAt
        }

    private fun targetEntity(
        goalId: UUID,
        currency: String,
    ): FinancialGoalTargetEntity =
        FinancialGoalTargetEntity(
            financialGoalId = goalId,
            currency = currency,
            targetAmount = BigDecimal("100.00"),
        ).also { it.id = UUID.randomUUID() }

    private fun movementEntity(
        goalId: UUID,
        walletItemId: UUID,
        signedAmount: BigDecimal,
    ): FinancialGoalLedgerMovementEntity =
        FinancialGoalLedgerMovementEntity(
            financialGoalId = goalId,
            walletItemId = walletItemId,
            signedAmount = signedAmount,
            note = null,
            movementKind = GoalLedgerMovementKind.IMMEDIATE,
            scheduleId = null,
            movementDate = LocalDate.of(2026, 4, 9),
        ).also {
            it.id = UUID.randomUUID()
            it.createdAt = OffsetDateTime.parse("2026-04-09T12:00:00Z")
            it.updatedAt = it.createdAt
        }

    private fun scheduleEntity(
        goalId: UUID,
        walletItemId: UUID,
    ): FinancialGoalContributionScheduleEntity =
        FinancialGoalContributionScheduleEntity(
            financialGoalId = goalId,
            walletItemId = walletItemId,
            amount = BigDecimal("20.00"),
            currency = "BRL",
            periodicity = RecurrenceType.MONTHLY,
            qtyExecuted = 0,
            qtyLimit = 6,
            lastExecution = null,
            nextExecution = LocalDate.of(2026, 5, 1),
            endExecution = LocalDate.of(2026, 10, 1),
            removesAllocation = false,
        ).also {
            it.id = UUID.randomUUID()
            it.createdAt = OffsetDateTime.parse("2026-04-09T12:00:00Z")
            it.updatedAt = it.createdAt
        }

    private fun bankWalletEntity(
        id: UUID,
        userId: UUID,
        currency: String,
    ): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.BANK_ACCOUNT,
            name = "Wallet $currency",
            enabled = true,
            userId = userId,
            currency = currency,
            balance = BigDecimal("1000.00"),
            totalLimit = null,
            dueDay = null,
            daysBetweenDueAndClosing = null,
            dueOnNextBusinessDay = null,
            showOnDashboard = true,
        ).also { it.id = id }

    private fun bankAccountModel(
        id: UUID,
        userId: UUID,
        currency: String,
        name: String,
    ): WalletItem =
        BankAccount(
            name = name,
            enabled = true,
            userId = userId,
            currency = currency,
            balance = BigDecimal("1000.00"),
            showOnDashboard = true,
        ).also { it.id = id }
}
