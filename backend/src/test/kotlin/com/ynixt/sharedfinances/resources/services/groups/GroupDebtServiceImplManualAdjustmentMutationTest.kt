package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
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
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class GroupDebtServiceImplManualAdjustmentMutationTest {
    @Test
    fun `edit manual adjustment should update the root movement directly and delete legacy compensations`() {
        runBlocking {
            val fixture = createFixture()
            val root =
                fixture.seedMovement(
                    deltaSigned = BigDecimal("100.00"),
                    reasonKind = GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT,
                    note = "Original",
                )
            fixture.seedMovement(
                deltaSigned = BigDecimal("-30.00"),
                reasonKind = GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT_COMPENSATION,
                note = "Compensation",
                sourceMovementId = root.id,
            )

            val updated =
                fixture.service.editManualAdjustment(
                    userId = fixture.actorUserId,
                    groupId = fixture.groupId,
                    movementId = requireNotNull(root.id),
                    input =
                        EditGroupDebtManualAdjustmentInput(
                            amountDelta = BigDecimal("90.00"),
                            note = "Edited",
                        ),
                )

            assertThat(updated.deltaSigned).isEqualByComparingTo("90.00")
            assertThat(updated.note).isEqualTo("Edited")
            assertThat(fixture.movements).hasSize(1)
            val persisted = fixture.movements.single()
            assertThat(persisted.id).isEqualTo(root.id)
            assertThat(persisted.deltaSigned).isEqualByComparingTo("90.00")
            assertThat(persisted.reasonKind).isEqualTo(GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT)
            assertThat(persisted.note).isEqualTo("Edited")
            assertThat(fixture.monthlyBalances.values).containsExactly(BigDecimal("90.00"))
        }
    }

    @Test
    fun `delete manual adjustment should remove the full legacy chain and clear monthly totals`() {
        runBlocking {
            val fixture = createFixture()
            val root =
                fixture.seedMovement(
                    deltaSigned = BigDecimal("100.00"),
                    reasonKind = GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT,
                    note = "Original",
                )
            fixture.seedMovement(
                deltaSigned = BigDecimal("-30.00"),
                reasonKind = GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT_COMPENSATION,
                note = "Compensation",
                sourceMovementId = root.id,
            )
            fixture.reconcileCurrentScope()

            fixture.service.deleteManualAdjustment(
                userId = fixture.actorUserId,
                groupId = fixture.groupId,
                movementId = requireNotNull(root.id),
            )

            assertThat(fixture.movements).isEmpty()
            assertThat(fixture.monthlyBalances).isEmpty()
        }
    }

    private fun createFixture(): Fixture {
        val movementRepository = inMemoryMovementRepository()
        val debtDatabaseClientRepository = inMemoryDebtDatabaseClientRepository(movementRepository.storage)
        val service =
            GroupDebtServiceImpl(
                groupPermissionService = AllowAllGroupPermissionService,
                movementRepository = movementRepository.repository,
                debtDatabaseClientRepository = debtDatabaseClientRepository.repository,
                walletEventRepository = Mockito.mock(WalletEventRepository::class.java),
                walletEventListService = Mockito.mock(WalletEventListService::class.java),
                recurrenceSimulationService = Mockito.mock(RecurrenceSimulationService::class.java),
                ledgerMaintenanceService =
                    GroupDebtLedgerMaintenanceService(
                        movementRepository.repository,
                        debtDatabaseClientRepository.repository,
                    ),
                clock = fixedClock(),
            )

        return Fixture(
            service = service,
            actorUserId = UUID.randomUUID(),
            groupId = UUID.randomUUID(),
            payerId = UUID.randomUUID(),
            receiverId = UUID.randomUUID(),
            movementRepository = movementRepository,
            debtDatabaseClientRepository = debtDatabaseClientRepository,
        )
    }

    private fun inMemoryMovementRepository(): InMemoryMovementRepository {
        val delegate = Mockito.mock(GroupMemberDebtMovementSpringDataRepository::class.java)
        val storage = linkedMapOf<UUID, GroupMemberDebtMovementEntity>()

        Mockito
            .doAnswer { invocation ->
                val persisted = invocation.arguments[0] as GroupMemberDebtMovementEntity
                if (persisted.id == null) {
                    persisted.id = UUID.randomUUID()
                }
                storage[requireNotNull(persisted.id)] = persisted
                Mono.just(persisted)
            }.`when`(delegate)
            .save(Mockito.any(GroupMemberDebtMovementEntity::class.java))

        val repository =
            object : GroupMemberDebtMovementSpringDataRepository by delegate {
                override fun deleteById(id: String): Mono<Void> {
                    storage.remove(UUID.fromString(id))
                    return Mono.empty()
                }

                override fun findByIdAndGroupId(
                    id: UUID,
                    groupId: UUID,
                ): Mono<GroupMemberDebtMovementEntity> = Mono.justOrEmpty(storage[id]?.takeIf { movement -> movement.groupId == groupId })

                override fun findAdjustmentChain(rootMovementId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(
                        storage.values
                            .filter { movement -> movement.id == rootMovementId || movement.sourceMovementId == rootMovementId }
                            .sortedBy { movement -> movement.createdAt },
                    )

                override fun findAllBySourceWalletEventId(sourceWalletEventId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(storage.values.filter { movement -> movement.sourceWalletEventId == sourceWalletEventId })

                override fun findAllBySourceMovementId(sourceMovementId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(storage.values.filter { movement -> movement.sourceMovementId == sourceMovementId })

                override fun findActiveBySourceWalletEventId(walletEventId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(storage.values.filter { movement -> movement.sourceWalletEventId == walletEventId })
            }

        return InMemoryMovementRepository(repository = repository, storage = storage)
    }

    private fun inMemoryDebtDatabaseClientRepository(
        storage: LinkedHashMap<UUID, GroupMemberDebtMovementEntity>,
    ): InMemoryDebtDatabaseClientRepository {
        val monthlyBalances = linkedMapOf<MonthlyBalanceKey, BigDecimal>()
        val repository =
            object : GroupMemberDebtDatabaseClientRepository(Mockito.mock(DatabaseClient::class.java)) {
                override fun sumMovementBalanceForScope(
                    groupId: UUID,
                    payerId: UUID,
                    receiverId: UUID,
                    month: LocalDate,
                    currency: String,
                ): Mono<BigDecimal> =
                    Mono.just(
                        storage.values
                            .filter { movement ->
                                movement.groupId == groupId &&
                                    movement.payerId == payerId &&
                                    movement.receiverId == receiverId &&
                                    movement.month == month &&
                                    movement.currency.equals(currency, ignoreCase = true)
                            }.fold(BigDecimal.ZERO) { acc, movement -> acc.add(movement.deltaSigned) },
                    )

                override fun upsertMonthlyBalance(
                    groupId: UUID,
                    payerId: UUID,
                    receiverId: UUID,
                    month: LocalDate,
                    currency: String,
                    balance: BigDecimal,
                ): Mono<Void> {
                    monthlyBalances[MonthlyBalanceKey(groupId, payerId, receiverId, month, currency.uppercase())] = balance
                    return Mono.empty()
                }

                override fun deleteMonthlyBalance(
                    groupId: UUID,
                    payerId: UUID,
                    receiverId: UUID,
                    month: LocalDate,
                    currency: String,
                ): Mono<Long> {
                    monthlyBalances.remove(MonthlyBalanceKey(groupId, payerId, receiverId, month, currency.uppercase()))
                    return Mono.just(1L)
                }
            }

        return InMemoryDebtDatabaseClientRepository(repository = repository, monthlyBalances = monthlyBalances)
    }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-10T00:00:00Z"), ZoneOffset.UTC)

    private data class Fixture(
        val service: GroupDebtServiceImpl,
        val actorUserId: UUID,
        val groupId: UUID,
        val payerId: UUID,
        val receiverId: UUID,
        val movementRepository: InMemoryMovementRepository,
        val debtDatabaseClientRepository: InMemoryDebtDatabaseClientRepository,
    ) {
        val movements: Collection<GroupMemberDebtMovementEntity>
            get() = movementRepository.storage.values

        val monthlyBalances: Map<MonthlyBalanceKey, BigDecimal>
            get() = debtDatabaseClientRepository.monthlyBalances

        fun seedMovement(
            deltaSigned: BigDecimal,
            reasonKind: GroupDebtMovementReasonKind,
            note: String?,
            sourceMovementId: UUID? = null,
            sourceWalletEventId: UUID? = null,
        ): GroupMemberDebtMovementEntity =
            GroupMemberDebtMovementEntity(
                groupId = groupId,
                payerId = payerId,
                receiverId = receiverId,
                month = LocalDate.of(2026, 7, 1),
                currency = "BRL",
                deltaSigned = deltaSigned,
                reasonKind = reasonKind,
                createdByUserId = actorUserId,
                note = note,
                sourceWalletEventId = sourceWalletEventId,
                sourceMovementId = sourceMovementId,
            ).also { movement ->
                movement.id = UUID.randomUUID()
                movement.createdAt = OffsetDateTime.parse("2026-07-10T00:00:00Z")
                movementRepository.storage[requireNotNull(movement.id)] = movement
            }

        fun reconcileCurrentScope() =
            runBlocking {
                GroupDebtLedgerMaintenanceService(
                    movementRepository.repository,
                    debtDatabaseClientRepository.repository,
                ).reconcileScopes(
                    movementRepository.storage.values.map { movement ->
                        GroupDebtLedgerMaintenanceService.MonthlyDebtScope(
                            groupId = movement.groupId,
                            payerId = movement.payerId,
                            receiverId = movement.receiverId,
                            month = movement.month,
                            currency = movement.currency,
                        )
                    },
                )
            }
    }

    private data class InMemoryMovementRepository(
        val repository: GroupMemberDebtMovementSpringDataRepository,
        val storage: LinkedHashMap<UUID, GroupMemberDebtMovementEntity>,
    )

    private data class InMemoryDebtDatabaseClientRepository(
        val repository: GroupMemberDebtDatabaseClientRepository,
        val monthlyBalances: LinkedHashMap<MonthlyBalanceKey, BigDecimal>,
    )

    private data class MonthlyBalanceKey(
        val groupId: UUID,
        val payerId: UUID,
        val receiverId: UUID,
        val month: LocalDate,
        val currency: String,
    )

    private object AllowAllGroupPermissionService : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ): Boolean = true

        override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> = emptySet()
    }
}
