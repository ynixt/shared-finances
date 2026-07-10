package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryRemovalService
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class GroupDebtSettlementDeletionServiceImplTest {
    @Test
    fun `delete settlement should remove the linked wallet entry and every derived debt fragment`() {
        runBlocking {
            val fixture = createFixture()
            val walletEventId = UUID.randomUUID()
            val march =
                fixture.seedSettlementMovement(
                    month = LocalDate.of(2026, 3, 1),
                    deltaSigned = BigDecimal("-100.00"),
                    sourceWalletEventId = walletEventId,
                )
            val april =
                fixture.seedSettlementMovement(
                    month = LocalDate.of(2026, 4, 1),
                    deltaSigned = BigDecimal("-50.00"),
                    sourceWalletEventId = walletEventId,
                )
            fixture.reconcileCurrentScopes()

            fixture.service.deleteSettlement(
                userId = fixture.actorUserId,
                groupId = fixture.groupId,
                movementId = requireNotNull(april.id),
            )

            assertThat(fixture.deletedWalletEventIds).containsExactly(walletEventId)
            assertThat(fixture.movements).doesNotContainKeys(requireNotNull(march.id), requireNotNull(april.id))
            assertThat(fixture.movements.values.map { movement -> movement.reasonKind })
                .doesNotContain(GroupDebtMovementReasonKind.DEBT_SETTLEMENT, GroupDebtMovementReasonKind.DEBT_SETTLEMENT_REVERSAL)
            assertThat(fixture.monthlyBalances).isEmpty()
        }
    }

    private fun createFixture(): Fixture {
        val movementRepository = inMemoryMovementRepository()
        val debtDatabaseClientRepository = inMemoryDebtDatabaseClientRepository(movementRepository.storage)
        val deletedWalletEventIds = mutableListOf<UUID>()

        val walletEntryRemovalService =
            object : WalletEntryRemovalService {
                override suspend fun deleteOneOff(
                    userId: UUID,
                    walletEventId: UUID,
                ) = error("deleteOneOff should not be used for debt settlement deletion")

                override suspend fun deleteOneOffWithoutDebtRollback(
                    userId: UUID,
                    walletEventId: UUID,
                ): WalletEventEntity {
                    deletedWalletEventIds += walletEventId

                    return WalletEventEntity(
                        type = WalletEntryType.TRANSFER,
                        name = "Debt settlement",
                        categoryId = null,
                        createdByUserId = userId,
                        groupId = movementRepository.groupId,
                        tags = null,
                        observations = null,
                        date = LocalDate.of(2026, 4, 10),
                        confirmed = true,
                        installment = null,
                        recurrenceEventId = null,
                        paymentType = PaymentType.UNIQUE,
                        transferPurpose = TransferPurpose.DEBT_SETTLEMENT,
                    ).also { event ->
                        event.id = walletEventId
                    }
                }

                override suspend fun deleteScheduled(
                    userId: UUID,
                    recurrenceConfigId: UUID,
                    request: com.ynixt.sharedfinances.domain.models.walletentry.DeleteScheduledEntryRequest,
                ) = null
            }

        val service =
            GroupDebtSettlementDeletionServiceImpl(
                groupPermissionService = AllowAllGroupPermissionService,
                movementRepository = movementRepository.repository,
                walletEntryRemovalService = walletEntryRemovalService,
                ledgerMaintenanceService =
                    GroupDebtLedgerMaintenanceService(
                        movementRepository.repository,
                        debtDatabaseClientRepository.repository,
                    ),
            )

        return Fixture(
            service = service,
            actorUserId = UUID.randomUUID(),
            groupId = movementRepository.groupId,
            payerId = movementRepository.payerId,
            receiverId = movementRepository.receiverId,
            movements = movementRepository.storage,
            repository = movementRepository.repository,
            debtRepository = debtDatabaseClientRepository.repository,
            monthlyBalances = debtDatabaseClientRepository.monthlyBalances,
            deletedWalletEventIds = deletedWalletEventIds,
        )
    }

    private fun inMemoryMovementRepository(): InMemoryMovementRepository {
        val delegate = Mockito.mock(GroupMemberDebtMovementSpringDataRepository::class.java)
        val storage = linkedMapOf<UUID, GroupMemberDebtMovementEntity>()
        val groupId = UUID.randomUUID()
        val payerId = UUID.randomUUID()
        val receiverId = UUID.randomUUID()

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
                        storage.values.filter { movement ->
                            movement.id == rootMovementId ||
                                movement.sourceMovementId == rootMovementId
                        },
                    )

                override fun findAllBySourceWalletEventId(sourceWalletEventId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(storage.values.filter { movement -> movement.sourceWalletEventId == sourceWalletEventId })

                override fun findAllBySourceMovementId(sourceMovementId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(storage.values.filter { movement -> movement.sourceMovementId == sourceMovementId })

                override fun findActiveBySourceWalletEventId(walletEventId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(storage.values.filter { movement -> movement.sourceWalletEventId == walletEventId })
            }

        return InMemoryMovementRepository(
            repository = repository,
            storage = storage,
            groupId = groupId,
            payerId = payerId,
            receiverId = receiverId,
        )
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

    private data class Fixture(
        val service: GroupDebtSettlementDeletionServiceImpl,
        val actorUserId: UUID,
        val groupId: UUID,
        val payerId: UUID,
        val receiverId: UUID,
        val movements: LinkedHashMap<UUID, GroupMemberDebtMovementEntity>,
        val repository: GroupMemberDebtMovementSpringDataRepository,
        val debtRepository: GroupMemberDebtDatabaseClientRepository,
        val monthlyBalances: LinkedHashMap<MonthlyBalanceKey, BigDecimal>,
        val deletedWalletEventIds: MutableList<UUID>,
    ) {
        fun seedSettlementMovement(
            month: LocalDate,
            deltaSigned: BigDecimal,
            sourceWalletEventId: UUID,
        ): GroupMemberDebtMovementEntity =
            GroupMemberDebtMovementEntity(
                groupId = groupId,
                payerId = payerId,
                receiverId = receiverId,
                month = month,
                currency = "BRL",
                deltaSigned = deltaSigned,
                reasonKind = GroupDebtMovementReasonKind.DEBT_SETTLEMENT,
                createdByUserId = actorUserId,
                sourceWalletEventId = sourceWalletEventId,
            ).also { movement ->
                movement.id = UUID.randomUUID()
                movement.createdAt = OffsetDateTime.now()
                movements[requireNotNull(movement.id)] = movement
            }

        fun reconcileCurrentScopes() =
            runBlocking {
                GroupDebtLedgerMaintenanceService(repository, debtRepository).reconcileScopes(
                    movements.values.map(ledgerScopeOf),
                )
            }

        private val ledgerScopeOf: (GroupMemberDebtMovementEntity) -> GroupDebtLedgerMaintenanceService.MonthlyDebtScope = { movement ->
            GroupDebtLedgerMaintenanceService.MonthlyDebtScope(
                groupId = movement.groupId,
                payerId = movement.payerId,
                receiverId = movement.receiverId,
                month = movement.month,
                currency = movement.currency,
            )
        }
    }

    private data class InMemoryMovementRepository(
        val repository: GroupMemberDebtMovementSpringDataRepository,
        val storage: LinkedHashMap<UUID, GroupMemberDebtMovementEntity>,
        val groupId: UUID,
        val payerId: UUID,
        val receiverId: UUID,
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
