package com.ynixt.sharedfinances.scenarios.support

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventBeneficiaryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventBeneficiaryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtHistoryFilter
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyCashFlow
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyComposition
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMovementLine
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtPairBalance
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtPairHistory
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtWorkspace
import com.ynixt.sharedfinances.domain.models.groups.debts.NewGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupMemberDebtDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupMemberDebtMovementSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.RecurrenceEventBeneficiarySpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEventBeneficiarySpringDataRepository
import org.mockito.Mockito
import org.mockito.Mockito.mock
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal object NoOpGroupDebtService : GroupDebtService {
    override suspend fun applyWalletEvent(
        actorUserId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
    ) = Unit

    override suspend fun rollbackWalletEvent(
        actorUserId: UUID,
        event: WalletEventEntity,
    ) = Unit

    override suspend fun getWorkspace(
        userId: UUID,
        groupId: UUID,
    ): GroupDebtWorkspace = GroupDebtWorkspace(balances = emptyList())

    override suspend fun listHistory(
        userId: UUID,
        groupId: UUID,
        filter: GroupDebtHistoryFilter,
    ): List<GroupDebtMovementLine> = emptyList()

    override suspend fun listPairHistory(
        userId: UUID,
        groupId: UUID,
        selectedMonth: YearMonth,
    ): List<GroupDebtPairHistory> = emptyList()

    override suspend fun getMovement(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
    ): GroupDebtMovementLine = error("Not used in tests")

    override suspend fun createManualAdjustment(
        userId: UUID,
        groupId: UUID,
        input: NewGroupDebtManualAdjustmentInput,
    ): GroupDebtMovementLine = error("Not used in tests")

    override suspend fun editManualAdjustment(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
        input: EditGroupDebtManualAdjustmentInput,
    ): GroupDebtMovementLine = error("Not used in tests")

    override suspend fun deleteManualAdjustment(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
    ) = Unit

    override suspend fun loadMonthlyCashFlow(
        groupId: UUID,
        scopedUserIds: Set<UUID>,
        fromMonth: YearMonth,
        toMonth: YearMonth,
    ): Map<Pair<YearMonth, String>, GroupDebtMonthlyCashFlow> = emptyMap()
}

internal interface ScenarioDebtLedgerBackedService {
    val scenarioDebtMovementRepository: GroupMemberDebtMovementSpringDataRepository
    val scenarioDebtDatabaseClientRepository: GroupMemberDebtDatabaseClientRepository
}

internal class ScenarioBackedGroupDebtService :
    GroupDebtService,
    ScenarioDebtLedgerBackedService {
    private val movements = linkedMapOf<UUID, GroupMemberDebtMovementEntity>()

    override val scenarioDebtMovementRepository: GroupMemberDebtMovementSpringDataRepository =
        run {
            val delegate = mock(GroupMemberDebtMovementSpringDataRepository::class.java)

            object : GroupMemberDebtMovementSpringDataRepository by delegate {
                override fun findAllBySourceWalletEventId(sourceWalletEventId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(
                        movements.values
                            .filter { movement -> movement.sourceWalletEventId == sourceWalletEventId }
                            .sortedWith(compareBy<GroupMemberDebtMovementEntity> { it.createdAt }.thenBy { it.id.toString() }),
                    )

                override fun findAllBySourceMovementId(sourceMovementId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    Flux.fromIterable(
                        movements.values
                            .filter { movement -> movement.sourceMovementId == sourceMovementId }
                            .sortedWith(compareBy<GroupMemberDebtMovementEntity> { it.createdAt }.thenBy { it.id.toString() }),
                    )

                override fun findActiveBySourceWalletEventId(walletEventId: UUID): Flux<GroupMemberDebtMovementEntity> =
                    findAllBySourceWalletEventId(walletEventId)

                override fun findByIdAndGroupId(
                    id: UUID,
                    groupId: UUID,
                ): Mono<GroupMemberDebtMovementEntity> = Mono.justOrEmpty(movements[id]?.takeIf { it.groupId == groupId })

                override fun findAdjustmentChain(rootMovementId: UUID): Flux<GroupMemberDebtMovementEntity> = Flux.empty()

                override fun deleteById(id: String): Mono<Void> {
                    movements.remove(UUID.fromString(id))
                    return Mono.empty()
                }
            }
        }

    override val scenarioDebtDatabaseClientRepository: GroupMemberDebtDatabaseClientRepository =
        Mockito.mock(GroupMemberDebtDatabaseClientRepository::class.java).also { mock ->
            Mockito
                .doAnswer { invocation ->
                    val groupId = invocation.getArgument<UUID>(0)
                    val payerId = invocation.getArgument<UUID>(1)
                    val receiverId = invocation.getArgument<UUID>(2)
                    val month = invocation.getArgument<LocalDate>(3)
                    val currency = invocation.getArgument<String>(4).uppercase()

                    Mono.just(
                        movements.values
                            .asSequence()
                            .filter { movement ->
                                movement.groupId == groupId &&
                                    movement.payerId == payerId &&
                                    movement.receiverId == receiverId &&
                                    movement.month == month &&
                                    movement.currency.uppercase() == currency
                            }.fold(BigDecimal.ZERO) { acc, movement -> acc.add(movement.deltaSigned) }
                            .setScale(2, RoundingMode.HALF_UP),
                    )
                }.`when`(mock)
                .sumMovementBalanceForScope(anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull())

            Mockito
                .doAnswer { Mono.empty<Void>() }
                .`when`(mock)
                .upsertMonthlyBalance(anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull())

            Mockito
                .doAnswer { Mono.just(1L) }
                .`when`(mock)
                .deleteMonthlyBalance(anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull())
        }

    fun seedCharge(
        groupId: UUID,
        payerId: UUID,
        receiverId: UUID,
        month: YearMonth,
        currency: String,
        amount: BigDecimal,
        createdByUserId: UUID = receiverId,
    ) {
        saveMovement(
            GroupMemberDebtMovementEntity(
                groupId = groupId,
                payerId = payerId,
                receiverId = receiverId,
                month = month.atDay(1),
                currency = currency.uppercase(),
                deltaSigned = amount.asMoney(),
                reasonKind = GroupDebtMovementReasonKind.BENEFICIARY_CHARGE,
                createdByUserId = createdByUserId,
            ),
        )
    }

    override suspend fun applyWalletEvent(
        actorUserId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
    ) {
        val groupId = event.groupId ?: return

        if (event.type == WalletEntryType.TRANSFER && event.transferPurpose == TransferPurpose.DEBT_SETTLEMENT) {
            val originEntry = entries.firstOrNull { entry -> entry.value < BigDecimal.ZERO } ?: return
            val targetEntry = entries.firstOrNull { entry -> entry.value > BigDecimal.ZERO } ?: return
            val payerId = originEntry.walletItem?.userId ?: return
            val receiverId = targetEntry.walletItem?.userId ?: return
            val currency = (originEntry.walletItem?.currency ?: targetEntry.walletItem?.currency ?: return).uppercase()
            val amount = originEntry.value.abs().asMoney()
            val month = earliestOpenMonth(groupId, payerId, receiverId, currency) ?: event.date.withDayOfMonth(1)

            saveMovement(
                GroupMemberDebtMovementEntity(
                    groupId = groupId,
                    payerId = payerId,
                    receiverId = receiverId,
                    month = month,
                    currency = currency,
                    deltaSigned = amount.negate(),
                    reasonKind = GroupDebtMovementReasonKind.DEBT_SETTLEMENT,
                    createdByUserId = actorUserId,
                    sourceWalletEventId = event.id,
                ),
            )
        }
    }

    override suspend fun rollbackWalletEvent(
        actorUserId: UUID,
        event: WalletEventEntity,
    ) {
        val eventId = event.id ?: return
        movements.entries.removeIf { (_, movement) -> movement.sourceWalletEventId == eventId }
    }

    override suspend fun getWorkspace(
        userId: UUID,
        groupId: UUID,
    ): GroupDebtWorkspace =
        GroupDebtWorkspace(
            balances =
                movements.values
                    .filter { movement -> movement.groupId == groupId }
                    .groupBy { movement -> Triple(movement.payerId, movement.receiverId, movement.currency.uppercase()) }
                    .mapNotNull { (pairKey, pairMovements) ->
                        val monthlyComposition =
                            pairMovements
                                .groupBy { movement -> YearMonth.from(movement.month) }
                                .toSortedMap()
                                .map { (month, monthMovements) ->
                                    GroupDebtMonthlyComposition(
                                        month = month,
                                        netAmount =
                                            monthMovements.fold(
                                                BigDecimal.ZERO.asMoney(),
                                            ) { acc, movement -> acc.add(movement.deltaSigned).asMoney() },
                                        chargeDelta =
                                            monthMovements
                                                .filter { it.reasonKind == GroupDebtMovementReasonKind.BENEFICIARY_CHARGE }
                                                .fold(
                                                    BigDecimal.ZERO.asMoney(),
                                                ) { acc, movement -> acc.add(movement.deltaSigned).asMoney() },
                                        settlementDelta =
                                            monthMovements
                                                .filter { it.reasonKind == GroupDebtMovementReasonKind.DEBT_SETTLEMENT }
                                                .fold(
                                                    BigDecimal.ZERO.asMoney(),
                                                ) { acc, movement -> acc.add(movement.deltaSigned).asMoney() },
                                        manualAdjustmentDelta = BigDecimal.ZERO.asMoney(),
                                    )
                                }

                        val outstandingAmount =
                            monthlyComposition.fold(BigDecimal.ZERO.asMoney()) { acc, month -> acc.add(month.netAmount).asMoney() }

                        if (outstandingAmount <= BigDecimal.ZERO) {
                            null
                        } else {
                            GroupDebtPairBalance(
                                payerId = pairKey.first,
                                receiverId = pairKey.second,
                                currency = pairKey.third,
                                outstandingAmount = outstandingAmount,
                                monthlyComposition = monthlyComposition,
                            )
                        }
                    }.sortedWith(compareBy<GroupDebtPairBalance>({ it.payerId.toString() }, { it.receiverId.toString() }, { it.currency })),
        )

    override suspend fun listHistory(
        userId: UUID,
        groupId: UUID,
        filter: GroupDebtHistoryFilter,
    ): List<GroupDebtMovementLine> = emptyList()

    override suspend fun listPairHistory(
        userId: UUID,
        groupId: UUID,
        selectedMonth: YearMonth,
    ): List<GroupDebtPairHistory> = emptyList()

    override suspend fun getMovement(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
    ): GroupDebtMovementLine = error("Not used in tests")

    override suspend fun createManualAdjustment(
        userId: UUID,
        groupId: UUID,
        input: NewGroupDebtManualAdjustmentInput,
    ): GroupDebtMovementLine = error("Not used in tests")

    override suspend fun editManualAdjustment(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
        input: EditGroupDebtManualAdjustmentInput,
    ): GroupDebtMovementLine = error("Not used in tests")

    override suspend fun deleteManualAdjustment(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
    ) = Unit

    override suspend fun loadMonthlyCashFlow(
        groupId: UUID,
        scopedUserIds: Set<UUID>,
        fromMonth: YearMonth,
        toMonth: YearMonth,
    ): Map<Pair<YearMonth, String>, GroupDebtMonthlyCashFlow> = emptyMap()

    private fun saveMovement(movement: GroupMemberDebtMovementEntity) {
        val movementId = movement.id ?: UUID.randomUUID().also { movement.id = it }
        movement.currency.uppercase()
        movements[movementId] = movement
    }

    private fun earliestOpenMonth(
        groupId: UUID,
        payerId: UUID,
        receiverId: UUID,
        currency: String,
    ): LocalDate? =
        movements.values
            .filter { movement ->
                movement.groupId == groupId &&
                    movement.payerId == payerId &&
                    movement.receiverId == receiverId &&
                    movement.currency.uppercase() == currency
            }.groupBy { movement -> movement.month }
            .toSortedMap()
            .entries
            .firstOrNull { (_, monthMovements) ->
                monthMovements.fold(BigDecimal.ZERO.asMoney()) { acc, movement -> acc.add(movement.deltaSigned).asMoney() } >
                    BigDecimal.ZERO
            }?.key

    private fun BigDecimal.asMoney(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}

@Suppress("UNCHECKED_CAST")
private fun <T> anyNonNull(): T = Mockito.any<T>() ?: null as T

internal fun inMemoryWalletEventBeneficiaryRepository(): WalletEventBeneficiarySpringDataRepository {
    val delegate = mock(WalletEventBeneficiarySpringDataRepository::class.java)
    val storage = linkedMapOf<UUID, MutableList<WalletEventBeneficiaryEntity>>()

    return object : WalletEventBeneficiarySpringDataRepository by delegate {
        override fun findAllByWalletEventId(walletEventId: UUID): Flux<WalletEventBeneficiaryEntity> =
            Flux.fromIterable(storage[walletEventId].orEmpty())

        override fun deleteAllByWalletEventId(walletEventId: UUID): Mono<Int> = Mono.just(storage.remove(walletEventId)?.size ?: 0)

        override fun <S : WalletEventBeneficiaryEntity> saveAll(entities: Iterable<S>): Flux<S> =
            Flux.fromIterable(
                entities.map { entity ->
                    entity.id = entity.id ?: UUID.randomUUID()
                    storage.getOrPut(entity.walletEventId) { mutableListOf() }.add(entity)
                    entity
                },
            )
    }
}

internal fun inMemoryRecurrenceEventBeneficiaryRepository(): RecurrenceEventBeneficiarySpringDataRepository {
    val delegate = mock(RecurrenceEventBeneficiarySpringDataRepository::class.java)
    val storage = linkedMapOf<UUID, MutableList<RecurrenceEventBeneficiaryEntity>>()

    return object : RecurrenceEventBeneficiarySpringDataRepository by delegate {
        override fun findAllByWalletEventId(walletEventId: UUID): Flux<RecurrenceEventBeneficiaryEntity> =
            Flux.fromIterable(storage[walletEventId].orEmpty())

        override fun deleteAllByWalletEventId(walletEventId: UUID): Mono<Int> = Mono.just(storage.remove(walletEventId)?.size ?: 0)

        override fun <S : RecurrenceEventBeneficiaryEntity> saveAll(entities: Iterable<S>): Flux<S> =
            Flux.fromIterable(
                entities.map { entity ->
                    entity.id = entity.id ?: UUID.randomUUID()
                    storage.getOrPut(entity.walletEventId) { mutableListOf() }.add(entity)
                    entity
                },
            )
    }
}
