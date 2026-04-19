package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
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
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.groups.debts.NewGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupMemberDebtDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupMemberDebtMovementSpringDataRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.StatementFilterFunction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID
import java.util.function.Supplier

class GroupDebtServiceImplTest {
    @Test
    fun `applyWalletEvent should derive beneficiary charges across multiple payers and beneficiaries`() =
        runBlocking {
            val actorUserId = UUID.randomUUID()
            val payerAId = UUID.randomUUID()
            val payerBId = UUID.randomUUID()
            val beneficiaryId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val eventId = UUID.randomUUID()
            val fixture = Fixture()

            fixture.service.applyWalletEvent(
                actorUserId = actorUserId,
                event =
                    expenseEvent(
                        id = eventId,
                        actorUserId = actorUserId,
                        groupId = groupId,
                        date = LocalDate.of(2026, 4, 10),
                        beneficiaries =
                            listOf(
                                beneficiary(userId = payerBId, percent = "50.00", walletEventId = eventId),
                                beneficiary(userId = beneficiaryId, percent = "50.00", walletEventId = eventId),
                            ),
                    ),
                entries =
                    listOf(
                        entry(
                            walletEventId = eventId,
                            walletItem = walletItem(userId = payerAId, currency = "BRL"),
                            value = "-60.00",
                        ),
                        entry(
                            walletEventId = eventId,
                            walletItem = walletItem(userId = payerBId, currency = "BRL"),
                            value = "-20.00",
                        ),
                    ),
            )

            assertThat(fixture.movementRepository.snapshot())
                .extracting("payerId", "receiverId", "deltaSigned", "reasonKind", "sourceWalletEventId")
                .containsExactlyInAnyOrder(
                    tuple(payerBId, payerAId, BigDecimal("20.00"), GroupDebtMovementReasonKind.BENEFICIARY_CHARGE, eventId),
                    tuple(beneficiaryId, payerAId, BigDecimal("40.00"), GroupDebtMovementReasonKind.BENEFICIARY_CHARGE, eventId),
                )
        }

    @Test
    fun `applyWalletEvent should record debt settlement in transfer month`() =
        runBlocking {
            val actorUserId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val eventId = UUID.randomUUID()
            val fixture = Fixture()

            fixture.service.applyWalletEvent(
                actorUserId = actorUserId,
                event =
                    transferEvent(
                        id = eventId,
                        actorUserId = actorUserId,
                        groupId = groupId,
                        date = LocalDate.of(2026, 5, 12),
                    ),
                entries =
                    listOf(
                        entry(
                            walletEventId = eventId,
                            walletItem = walletItem(userId = payerId, currency = "BRL"),
                            value = "-25.00",
                        ),
                        entry(
                            walletEventId = eventId,
                            walletItem = walletItem(userId = receiverId, currency = "BRL"),
                            value = "25.00",
                        ),
                    ),
            )

            assertThat(fixture.movementRepository.snapshot())
                .singleElement()
                .satisfies({ movement ->
                    assertThat(movement.payerId).isEqualTo(payerId)
                    assertThat(movement.receiverId).isEqualTo(receiverId)
                    assertThat(movement.month).isEqualTo(LocalDate.of(2026, 5, 1))
                    assertThat(movement.deltaSigned).isEqualByComparingTo("-25.00")
                    assertThat(movement.reasonKind).isEqualTo(GroupDebtMovementReasonKind.DEBT_SETTLEMENT)
                    assertThat(movement.sourceWalletEventId).isEqualTo(eventId)
                })
        }

    @Test
    fun `rollbackWalletEvent should append reversal for active source movements`() =
        runBlocking {
            val actorUserId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val eventId = UUID.randomUUID()
            val fixture = Fixture()
            val event =
                expenseEvent(
                    id = eventId,
                    actorUserId = actorUserId,
                    groupId = groupId,
                    date = LocalDate.of(2026, 4, 10),
                    beneficiaries = listOf(beneficiary(userId = payerId, percent = "100.00", walletEventId = eventId)),
                )

            fixture.service.applyWalletEvent(
                actorUserId = actorUserId,
                event = event,
                entries =
                    listOf(
                        entry(
                            walletEventId = eventId,
                            walletItem = walletItem(userId = receiverId, currency = "BRL"),
                            value = "-30.00",
                        ),
                    ),
            )
            fixture.service.rollbackWalletEvent(actorUserId = actorUserId, event = event)

            val movements = fixture.movementRepository.snapshot()

            assertThat(movements).hasSize(2)
            assertThat(movements[0].reasonKind).isEqualTo(GroupDebtMovementReasonKind.BENEFICIARY_CHARGE)
            assertThat(movements[0].deltaSigned).isEqualByComparingTo("30.00")
            assertThat(movements[1].reasonKind).isEqualTo(GroupDebtMovementReasonKind.BENEFICIARY_REVERSAL)
            assertThat(movements[1].deltaSigned).isEqualByComparingTo("-30.00")
            assertThat(movements[1].sourceMovementId).isEqualTo(movements[0].id)
        }

    @Test
    fun `editManualAdjustment should append compensation instead of overwriting root movement`() =
        runBlocking {
            val actorUserId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val fixture = Fixture()

            val created =
                fixture.service.createManualAdjustment(
                    userId = actorUserId,
                    groupId = groupId,
                    input =
                        NewGroupDebtManualAdjustmentInput(
                            payerId = payerId,
                            receiverId = receiverId,
                            month = YearMonth.of(2026, 4),
                            currency = "brl",
                            amountDelta = BigDecimal("15.00"),
                            note = "initial",
                        ),
                )

            val edited =
                fixture.service.editManualAdjustment(
                    userId = actorUserId,
                    groupId = groupId,
                    movementId = created.id,
                    input =
                        EditGroupDebtManualAdjustmentInput(
                            amountDelta = BigDecimal("9.00"),
                            note = "correction",
                        ),
                )

            val movements = fixture.movementRepository.snapshot()

            assertThat(movements).hasSize(2)
            assertThat(movements[0].reasonKind).isEqualTo(GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT)
            assertThat(movements[0].deltaSigned).isEqualByComparingTo("15.00")
            assertThat(movements[1].reasonKind).isEqualTo(GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT_COMPENSATION)
            assertThat(movements[1].deltaSigned).isEqualByComparingTo("-6.00")
            assertThat(movements[1].sourceMovementId).isEqualTo(movements[0].id)
            assertThat(edited.id).isEqualTo(movements[1].id)
        }

    private class Fixture(
        permissionAllowed: Boolean = true,
    ) {
        val movementRepository = InMemoryGroupMemberDebtMovementRepository()

        val service =
            GroupDebtServiceImpl(
                groupPermissionService = FakeGroupPermissionService(permissionAllowed),
                movementRepository = movementRepository,
                debtDatabaseClientRepository = GroupMemberDebtDatabaseClientRepository(buildNoOpDatabaseClient()),
            )
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

    private class InMemoryGroupMemberDebtMovementRepository :
        GroupMemberDebtMovementSpringDataRepository by mock(GroupMemberDebtMovementSpringDataRepository::class.java) {
        private val data = mutableListOf<GroupMemberDebtMovementEntity>()

        fun snapshot(): List<GroupMemberDebtMovementEntity> = data.toList()

        override fun <S : GroupMemberDebtMovementEntity> save(entity: S): Mono<S> {
            entity.id = entity.id ?: UUID.randomUUID()
            entity.createdAt = entity.createdAt ?: OffsetDateTime.parse("2026-04-10T12:00:00Z").plusSeconds(data.size.toLong())
            data += entity
            return Mono.just(entity)
        }

        override fun findActiveBySourceWalletEventId(walletEventId: UUID): Flux<GroupMemberDebtMovementEntity> =
            Flux.fromIterable(
                data.filter { movement ->
                    movement.sourceWalletEventId == walletEventId &&
                        data.none { reversal -> reversal.sourceMovementId == movement.id }
                },
            )

        override fun findByIdAndGroupId(
            id: UUID,
            groupId: UUID,
        ): Mono<GroupMemberDebtMovementEntity> =
            Mono.justOrEmpty(data.firstOrNull { movement -> movement.id == id && movement.groupId == groupId })

        override fun findAdjustmentChain(rootMovementId: UUID): Flux<GroupMemberDebtMovementEntity> =
            Flux.fromIterable(
                data
                    .filter { movement -> movement.id == rootMovementId || movement.sourceMovementId == rootMovementId }
                    .sortedWith(compareBy<GroupMemberDebtMovementEntity> { it.createdAt }.thenBy { it.id }),
            )
    }

    private fun expenseEvent(
        id: UUID,
        actorUserId: UUID,
        groupId: UUID,
        date: LocalDate,
        beneficiaries: List<WalletEventBeneficiaryEntity>,
    ): WalletEventEntity =
        WalletEventEntity(
            type = WalletEntryType.EXPENSE,
            name = "Shared expense",
            categoryId = null,
            createdByUserId = actorUserId,
            groupId = groupId,
            tags = emptyList(),
            observations = null,
            date = date,
            confirmed = true,
            installment = null,
            recurrenceEventId = null,
            paymentType = PaymentType.UNIQUE,
        ).also { event ->
            event.id = id
            event.beneficiaries = beneficiaries.also { items -> items.forEach { it.event = event } }
        }

    private fun transferEvent(
        id: UUID,
        actorUserId: UUID,
        groupId: UUID,
        date: LocalDate,
    ): WalletEventEntity =
        WalletEventEntity(
            type = WalletEntryType.TRANSFER,
            name = "Debt settlement",
            categoryId = null,
            createdByUserId = actorUserId,
            groupId = groupId,
            tags = emptyList(),
            observations = null,
            date = date,
            confirmed = true,
            installment = null,
            recurrenceEventId = null,
            paymentType = PaymentType.UNIQUE,
            transferPurpose = TransferPurpose.DEBT_SETTLEMENT,
        ).also { it.id = id }

    private fun beneficiary(
        userId: UUID,
        percent: String,
        walletEventId: UUID,
    ): WalletEventBeneficiaryEntity =
        WalletEventBeneficiaryEntity(
            walletEventId = walletEventId,
            beneficiaryUserId = userId,
            benefitPercent = BigDecimal(percent),
        )

    private fun entry(
        walletEventId: UUID,
        walletItem: WalletItemEntity,
        value: String,
    ): WalletEntryEntity =
        WalletEntryEntity(
            value = BigDecimal(value),
            walletEventId = walletEventId,
            walletItemId = requireNotNull(walletItem.id),
            billId = null,
        ).also { entry ->
            entry.id = UUID.randomUUID()
            entry.walletItem = walletItem
        }

    private fun walletItem(
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
        ).also { it.id = UUID.randomUUID() }
}

private fun buildNoOpDatabaseClient(): DatabaseClient {
    val clientDelegate = mock(DatabaseClient::class.java)
    val specDelegate = mock(DatabaseClient.GenericExecuteSpec::class.java)
    val spec =
        object : DatabaseClient.GenericExecuteSpec by specDelegate {
            override fun bind(
                index: Int,
                value: Any,
            ): DatabaseClient.GenericExecuteSpec = this

            override fun bindNull(
                index: Int,
                type: Class<*>,
            ): DatabaseClient.GenericExecuteSpec = this

            override fun bind(
                name: String,
                value: Any,
            ): DatabaseClient.GenericExecuteSpec = this

            override fun bindNull(
                name: String,
                type: Class<*>,
            ): DatabaseClient.GenericExecuteSpec = this

            override fun bindValues(source: List<*>): DatabaseClient.GenericExecuteSpec = this

            override fun bindValues(source: Map<String, *>): DatabaseClient.GenericExecuteSpec = this

            override fun bindProperties(source: Any): DatabaseClient.GenericExecuteSpec = this

            override fun filter(filter: StatementFilterFunction): DatabaseClient.GenericExecuteSpec = this

            override fun then(): Mono<Void> = Mono.empty()
        }

    return object : DatabaseClient by clientDelegate {
        override fun sql(sql: String): DatabaseClient.GenericExecuteSpec = spec

        override fun sql(sqlSupplier: Supplier<String>): DatabaseClient.GenericExecuteSpec = spec
    }
}
