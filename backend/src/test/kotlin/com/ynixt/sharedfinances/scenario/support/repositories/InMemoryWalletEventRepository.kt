package com.ynixt.sharedfinances.scenario.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.scenario.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

internal class InMemoryWalletEventRepository : WalletEventRepository {
    private val data = linkedMapOf<UUID, WalletEventEntity>()

    override fun save(walletEntry: WalletEventEntity): Mono<WalletEventEntity> {
        val id = walletEntry.id ?: UUID.randomUUID()
        walletEntry.id = id
        walletEntry.createdAt = walletEntry.createdAt ?: nowOffset()
        walletEntry.updatedAt = nowOffset()
        data[id] = walletEntry
        return Mono.just(walletEntry)
    }

    override fun saveAll(walletEntry: Iterable<WalletEventEntity>): Flux<WalletEventEntity> =
        Flux.fromIterable(walletEntry).flatMap { save(it) }

    override fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long> {
        val initial = data.size
        data.entries.removeIf { (_, value) -> value.userId == userId }
        return Mono.just((initial - data.size).toLong())
    }

    override fun findAll(
        userId: UUID?,
        groupId: UUID?,
        limit: Int,
        walletItemId: UUID?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): Flux<WalletEventEntity> {
        val filtered =
            data.values
                .asSequence()
                .filter { userId == null || it.userId == userId }
                .filter { groupId == null || it.groupId == groupId }
                .filter { minimumDate == null || !it.date.isBefore(minimumDate) }
                .filter { maximumDate == null || !it.date.isAfter(maximumDate) }
                .take(limit)
                .toList()

        return Flux.fromIterable(filtered)
    }
}
