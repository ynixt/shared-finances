package com.ynixt.sharedfinances.scenarios.support

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventBeneficiaryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventBeneficiaryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtHistoryFilter
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyCashFlow
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMovementLine
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtWorkspace
import com.ynixt.sharedfinances.domain.models.groups.debts.NewGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.RecurrenceEventBeneficiarySpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEventBeneficiarySpringDataRepository
import org.mockito.Mockito.mock
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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

    override suspend fun loadMonthlyCashFlow(
        groupId: UUID,
        scopedUserIds: Set<UUID>,
        fromMonth: YearMonth,
        toMonth: YearMonth,
    ): Map<Pair<YearMonth, String>, GroupDebtMonthlyCashFlow> = emptyMap()
}

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
