package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

internal class InMemoryRecurrenceEntryRepository : RecurrenceEntryRepository {
    private val data = linkedMapOf<UUID, RecurrenceEntryEntity>()

    fun deleteAllByWalletEventIds(walletEventIds: Set<UUID>): Int {
        val initial = data.size
        data.entries.removeIf { (_, entry) -> walletEventIds.contains(entry.walletEventId) }
        return initial - data.size
    }

    override fun updateNextBillDate(
        id: UUID,
        nextBillDate: LocalDate?,
    ): Mono<Int> {
        val current = data[id] ?: return Mono.just(0)
        data[id] = copyRecurrenceEntry(current, nextBillDate = nextBillDate)
        return Mono.just(1)
    }

    override fun findAllByWalletEventId(walletEventId: UUID): Flux<RecurrenceEntryEntity> =
        Flux.fromIterable(data.values.filter { it.walletEventId == walletEventId })

    override fun findById(id: UUID): Mono<RecurrenceEntryEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(if (data.remove(id) != null) 1L else 0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(data.containsKey(id))

    override fun <S : RecurrenceEntryEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        data[id] = entity
        return Mono.just(entity)
    }

    override fun <S : RecurrenceEntryEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<RecurrenceEntryEntity> = Flux.fromIterable(id.mapNotNull { data[it] })

    private fun copyRecurrenceEntry(
        current: RecurrenceEntryEntity,
        nextBillDate: LocalDate?,
    ): RecurrenceEntryEntity =
        RecurrenceEntryEntity(
            value = current.value,
            walletEventId = current.walletEventId,
            walletItemId = current.walletItemId,
            nextBillDate = nextBillDate,
            lastBillDate = current.lastBillDate,
        ).also {
            it.id = current.id
            it.event = current.event
            it.walletItem = current.walletItem
            it.createdAt = current.createdAt
            it.updatedAt = nowOffset()
        }
}
