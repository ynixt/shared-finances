package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventBeneficiaryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidDebtSettlementException
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupMemberDebtDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupMemberDebtMovementSpringDataRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

class GroupDebtServiceImplSettlementAllocationTest {
    @Test
    fun `should settle the oldest open competence first`() =
        runBlocking {
            val fixture =
                createFixture(
                    openBalances =
                        listOf(
                            openBalance(YearMonth.of(2026, 3), "2000.00"),
                            openBalance(YearMonth.of(2026, 4), "1500.00"),
                        ),
                )

            fixture.service.applyWalletEvent(
                actorUserId = fixture.actorUserId,
                event = settlementEvent(fixture, LocalDate.of(2026, 4, 15)),
                entries = settlementEntries(fixture, "-2000.00"),
            )

            assertThat(fixture.savedMovements).hasSize(1)
            val movement = fixture.savedMovements.single()
            assertThat(movement.month).isEqualTo(LocalDate.of(2026, 3, 1))
            assertThat(movement.deltaSigned).isEqualByComparingTo("-2000.00")
            assertThat(movement.reasonKind).isEqualTo(GroupDebtMovementReasonKind.DEBT_SETTLEMENT)
            assertThat(movement.sourceWalletEventId).isNotNull
        }

    @Test
    fun `should split settlement across multiple competences and keep one payment source`() =
        runBlocking {
            val fixture =
                createFixture(
                    openBalances =
                        listOf(
                            openBalance(YearMonth.of(2026, 3), "2000.00"),
                            openBalance(YearMonth.of(2026, 4), "1500.00"),
                            openBalance(YearMonth.of(2026, 5), "1000.00"),
                        ),
                )

            val event = settlementEvent(fixture, LocalDate.of(2026, 4, 15))
            fixture.service.applyWalletEvent(
                actorUserId = fixture.actorUserId,
                event = event,
                entries = settlementEntries(fixture, "-4000.00"),
            )

            assertThat(fixture.savedMovements).hasSize(3)
            assertThat(fixture.savedMovements.map { it.month })
                .containsExactly(
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 5, 1),
                )
            assertThat(fixture.savedMovements.map { it.deltaSigned })
                .containsExactly(
                    BigDecimal("-2000.00"),
                    BigDecimal("-1500.00"),
                    BigDecimal("-500.00"),
                )
            assertThat(fixture.savedMovements.map { it.sourceWalletEventId }.distinct()).containsExactly(event.id)
        }

    @Test
    fun `should reject settlement amount that exceeds open debt`() =
        runBlocking {
            val fixture =
                createFixture(
                    openBalances =
                        listOf(
                            openBalance(YearMonth.of(2026, 3), "2000.00"),
                            openBalance(YearMonth.of(2026, 4), "1500.00"),
                            openBalance(YearMonth.of(2026, 5), "1000.00"),
                        ),
                )

            assertThatThrownBy {
                runBlocking {
                    fixture.service.applyWalletEvent(
                        actorUserId = fixture.actorUserId,
                        event = settlementEvent(fixture, LocalDate.of(2026, 4, 15)),
                        entries = settlementEntries(fixture, "-5000.00"),
                    )
                }
            }.isInstanceOf(InvalidDebtSettlementException::class.java)

            assertThat(fixture.savedMovements).isEmpty()
        }

    @Test
    fun `rollback should mirror every active settlement fragment individually`() =
        runBlocking {
            val fixture = createFixture(openBalances = emptyList())
            val sourceEventId = fixture.rollbackSourceEventId
            val originalFirst = savedSettlementFragment(fixture, sourceEventId, YearMonth.of(2026, 3), "-2000.00")
            val originalSecond = savedSettlementFragment(fixture, sourceEventId, YearMonth.of(2026, 4), "-1000.00")
            fixture.activeMovements += listOf(originalFirst, originalSecond)

            fixture.service.rollbackWalletEvent(
                actorUserId = fixture.actorUserId,
                event = settlementEvent(fixture, LocalDate.of(2026, 4, 15), sourceEventId),
            )

            assertThat(fixture.savedMovements).hasSize(2)
            assertThat(fixture.savedMovements.map { it.month })
                .containsExactly(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1))
            assertThat(fixture.savedMovements.map { it.deltaSigned })
                .containsExactly(BigDecimal("2000.00"), BigDecimal("1000.00"))
            assertThat(fixture.savedMovements.map { it.reasonKind }.distinct())
                .containsExactly(GroupDebtMovementReasonKind.DEBT_SETTLEMENT_REVERSAL)
            assertThat(fixture.savedMovements.map { it.sourceMovementId })
                .containsExactly(originalFirst.id, originalSecond.id)
        }

    @Test
    fun `credit card charges should use the bill month as debt competence`() =
        runBlocking {
            val fixture = createFixture(openBalances = emptyList())
            val eventDate = LocalDate.of(2026, 5, 25)
            val billDate = LocalDate.of(2026, 6, 1)

            fixture.service.applyWalletEvent(
                actorUserId = fixture.actorUserId,
                event = expenseEvent(fixture, eventDate),
                entries = expenseEntries(fixture, "-40.00", billDate),
            )

            assertThat(fixture.savedMovements).hasSize(1)
            val movement = fixture.savedMovements.single()
            assertThat(movement.month).isEqualTo(billDate)
            assertThat(movement.deltaSigned).isEqualByComparingTo("20.00")
            assertThat(movement.reasonKind).isEqualTo(GroupDebtMovementReasonKind.BENEFICIARY_CHARGE)
        }

    private fun createFixture(openBalances: List<GroupMemberDebtDatabaseClientRepository.OpenDebtBalanceRow>): Fixture {
        val movementRepository = Mockito.mock(GroupMemberDebtMovementSpringDataRepository::class.java)
        val actorUserId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val payerUserId = UUID.randomUUID()
        val receiverUserId = UUID.randomUUID()
        val originWallet = bankWallet(payerUserId, "Origin wallet")
        val targetWallet = bankWallet(receiverUserId, "Target wallet")
        val creditCardWallet = creditCardWallet(payerUserId, "Credit card")
        val savedMovements = mutableListOf<com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity>()
        val activeMovements = mutableListOf<com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity>()
        val rollbackSourceEventId = UUID.randomUUID()
        val debtDatabaseClientRepository =
            FakeDebtDatabaseClientRepository(
                expectedGroupId = groupId,
                expectedPayerId = payerUserId,
                expectedReceiverId = receiverUserId,
                openBalances = openBalances,
            )

        Mockito
            .doAnswer { invocation ->
                val movement =
                    invocation.arguments[0] as com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
                if (movement.id == null) {
                    movement.id = UUID.randomUUID()
                }
                savedMovements += movement
                Mono.just(movement)
            }.`when`(movementRepository)
            .save(any())
        Mockito
            .doAnswer { Flux.fromIterable(activeMovements) }
            .`when`(movementRepository)
            .findActiveBySourceWalletEventId(rollbackSourceEventId)

        val service =
            GroupDebtServiceImpl(
                groupPermissionService = AllowAllGroupPermissionService,
                movementRepository = movementRepository,
                debtDatabaseClientRepository = debtDatabaseClientRepository,
                walletEventRepository = Mockito.mock(WalletEventRepository::class.java),
                walletEventListService = Mockito.mock(WalletEventListService::class.java),
                recurrenceSimulationService = Mockito.mock(RecurrenceSimulationService::class.java),
                clock = fixedClock(),
            )

        return Fixture(
            service = service,
            actorUserId = actorUserId,
            groupId = groupId,
            payerUserId = payerUserId,
            receiverUserId = receiverUserId,
            rollbackSourceEventId = rollbackSourceEventId,
            originWallet = originWallet,
            targetWallet = targetWallet,
            creditCardWallet = creditCardWallet,
            savedMovements = savedMovements,
            activeMovements = activeMovements,
        )
    }

    private fun settlementEvent(
        fixture: Fixture,
        date: LocalDate,
        id: UUID = UUID.randomUUID(),
    ) = WalletEventEntity(
        type = WalletEntryType.TRANSFER,
        name = "Debt settlement",
        categoryId = null,
        createdByUserId = fixture.actorUserId,
        groupId = fixture.groupId,
        tags = null,
        observations = null,
        date = date,
        confirmed = true,
        installment = null,
        recurrenceEventId = null,
        paymentType = PaymentType.UNIQUE,
        transferPurpose = TransferPurpose.DEBT_SETTLEMENT,
    ).also {
        it.id = id
    }

    private fun settlementEntries(
        fixture: Fixture,
        originValue: String,
    ): List<WalletEntryEntity> {
        val origin =
            WalletEntryEntity(
                value = BigDecimal(originValue),
                walletEventId = UUID.randomUUID(),
                walletItemId = requireNotNull(fixture.originWallet.id),
                billId = null,
            ).also { entry ->
                entry.walletItem = fixture.originWallet
            }

        val target =
            WalletEntryEntity(
                value = BigDecimal(originValue).abs(),
                walletEventId = UUID.randomUUID(),
                walletItemId = requireNotNull(fixture.targetWallet.id),
                billId = null,
            ).also { entry ->
                entry.walletItem = fixture.targetWallet
            }

        return listOf(origin, target)
    }

    private fun expenseEvent(
        fixture: Fixture,
        date: LocalDate,
    ) = WalletEventEntity(
        type = WalletEntryType.EXPENSE,
        name = "Installment expense",
        categoryId = null,
        createdByUserId = fixture.actorUserId,
        groupId = fixture.groupId,
        tags = null,
        observations = null,
        date = date,
        confirmed = true,
        installment = 2,
        recurrenceEventId = null,
        paymentType = PaymentType.INSTALLMENTS,
    ).also { event ->
        event.id = UUID.randomUUID()
        event.beneficiaries =
            listOf(
                WalletEventBeneficiaryEntity(
                    walletEventId = event.id!!,
                    beneficiaryUserId = fixture.receiverUserId,
                    benefitPercent = BigDecimal("50.00"),
                ),
                WalletEventBeneficiaryEntity(
                    walletEventId = event.id!!,
                    beneficiaryUserId = fixture.payerUserId,
                    benefitPercent = BigDecimal("50.00"),
                ),
            )
    }

    private fun expenseEntries(
        fixture: Fixture,
        value: String,
        billDate: LocalDate,
    ): List<WalletEntryEntity> =
        listOf(
            WalletEntryEntity(
                value = BigDecimal(value),
                walletEventId = UUID.randomUUID(),
                walletItemId = requireNotNull(fixture.creditCardWallet.id),
                billId = UUID.randomUUID(),
            ).also { entry ->
                entry.walletItem = fixture.creditCardWallet
                entry.bill =
                    CreditCardBillEntity(
                        creditCardId = requireNotNull(fixture.creditCardWallet.id),
                        billDate = billDate,
                        dueDate = billDate.plusDays(10),
                        closingDate = billDate.minusDays(5),
                        paid = false,
                        value = BigDecimal(value),
                    )
            },
        )

    private fun savedSettlementFragment(
        fixture: Fixture,
        sourceEventId: UUID,
        month: YearMonth,
        delta: String,
    ) = com.ynixt.sharedfinances.domain.entities.groups
        .GroupMemberDebtMovementEntity(
            groupId = fixture.groupId,
            payerId = fixture.payerUserId,
            receiverId = fixture.receiverUserId,
            month = month.atDay(1),
            currency = "BRL",
            deltaSigned = BigDecimal(delta),
            reasonKind = GroupDebtMovementReasonKind.DEBT_SETTLEMENT,
            createdByUserId = fixture.actorUserId,
            sourceWalletEventId = sourceEventId,
        ).also {
            it.id = UUID.randomUUID()
            it.createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        }

    private fun openBalance(
        month: YearMonth,
        balance: String,
    ) = GroupMemberDebtDatabaseClientRepository.OpenDebtBalanceRow(
        month = month,
        balance = BigDecimal(balance),
    )

    private fun bankWallet(
        ownerId: UUID,
        name: String,
    ) = WalletItemEntity(
        type = WalletItemType.BANK_ACCOUNT,
        name = name,
        enabled = true,
        userId = ownerId,
        currency = "BRL",
        balance = BigDecimal("1000.00"),
        totalLimit = null,
        dueDay = null,
        daysBetweenDueAndClosing = null,
        dueOnNextBusinessDay = null,
    ).also {
        it.id = UUID.randomUUID()
    }

    private fun creditCardWallet(
        ownerId: UUID,
        name: String,
    ) = WalletItemEntity(
        type = WalletItemType.CREDIT_CARD,
        name = name,
        enabled = true,
        userId = ownerId,
        currency = "BRL",
        balance = BigDecimal.ZERO,
        totalLimit = BigDecimal("5000.00"),
        dueDay = 10,
        daysBetweenDueAndClosing = 5,
        dueOnNextBusinessDay = false,
    ).also {
        it.id = UUID.randomUUID()
    }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneOffset.UTC)

    private data class Fixture(
        val service: GroupDebtServiceImpl,
        val actorUserId: UUID,
        val groupId: UUID,
        val payerUserId: UUID,
        val receiverUserId: UUID,
        val rollbackSourceEventId: UUID,
        val originWallet: WalletItemEntity,
        val targetWallet: WalletItemEntity,
        val creditCardWallet: WalletItemEntity,
        val savedMovements: MutableList<com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity>,
        val activeMovements: MutableList<com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity>,
    )

    private object AllowAllGroupPermissionService : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ): Boolean = true

        override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> = emptySet()
    }

    private class FakeDebtDatabaseClientRepository(
        private val expectedGroupId: UUID,
        private val expectedPayerId: UUID,
        private val expectedReceiverId: UUID,
        private val openBalances: List<GroupMemberDebtDatabaseClientRepository.OpenDebtBalanceRow>,
    ) : GroupMemberDebtDatabaseClientRepository(Mockito.mock(DatabaseClient::class.java)) {
        override fun listOpenBalancesForPair(
            groupId: UUID,
            payerId: UUID,
            receiverId: UUID,
            currency: String,
        ): Flux<OpenDebtBalanceRow> {
            require(groupId == expectedGroupId)
            require(payerId == expectedPayerId)
            require(receiverId == expectedReceiverId)
            require(currency == "BRL")
            return Flux.fromIterable(openBalances)
        }

        override fun upsertMonthlyDelta(movement: GroupMemberDebtMovementEntity): Mono<Void> = Mono.empty()
    }
}
