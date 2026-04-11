package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

internal class InMemoryWalletEventRepository : WalletEventRepository {
    private val data = linkedMapOf<UUID, WalletEventEntity>()

    fun snapshot(): List<WalletEventEntity> = data.values.toList()

    fun getOrNull(id: UUID): WalletEventEntity? = data[id]

    fun findIdsByRecurrenceEventIds(recurrenceEventIds: Set<UUID>): Set<UUID> =
        data.values
            .filter { it.recurrenceEventId != null && recurrenceEventIds.contains(it.recurrenceEventId) }
            .mapNotNull { it.id }
            .toSet()

    fun deleteAllByRecurrenceEventIds(recurrenceEventIds: Set<UUID>): Long {
        val initial = data.size
        data.entries.removeIf { (_, value) -> value.recurrenceEventId != null && recurrenceEventIds.contains(value.recurrenceEventId) }
        return (initial - data.size).toLong()
    }

    override fun findById(id: UUID): Mono<WalletEventEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(if (data.remove(id) != null) 1L else 0L)

    override fun findOneByRecurrenceEventIdAndDate(
        recurrenceEventId: UUID,
        date: LocalDate,
    ): Mono<WalletEventEntity> =
        Mono.justOrEmpty(
            data.values.firstOrNull { it.recurrenceEventId == recurrenceEventId && it.date == date },
        )

    override fun findAllByRecurrenceEventId(recurrenceEventId: UUID): Flux<WalletEventEntity> =
        Flux.fromIterable(data.values.filter { it.recurrenceEventId == recurrenceEventId })

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
