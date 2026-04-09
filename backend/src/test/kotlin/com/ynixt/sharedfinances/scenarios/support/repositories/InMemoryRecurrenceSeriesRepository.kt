package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceSeriesEntity
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

internal class InMemoryRecurrenceSeriesRepository(
    private val onDeleteSeries: ((UUID) -> Unit)? = null,
) : RecurrenceSeriesRepository {
    private val data = linkedMapOf<UUID, RecurrenceSeriesEntity>()

    override fun updateQtyTotal(
        id: UUID,
        qtyTotal: Int?,
    ): Mono<Long> {
        val current = data[id] ?: return Mono.just(0L)
        data[id] = copySeries(current, qtyTotal = qtyTotal)
        return Mono.just(1L)
    }

    override fun incrementQtyTotal(
        id: UUID,
        amount: Int,
    ): Mono<Long> {
        val current = data[id] ?: return Mono.just(0L)
        val currentValue = current.qtyTotal ?: 0
        data[id] = copySeries(current, qtyTotal = currentValue + amount)
        return Mono.just(1L)
    }

    override fun findById(id: UUID): Mono<RecurrenceSeriesEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> {
        val removed = data.remove(id)
        if (removed != null) {
            onDeleteSeries?.invoke(id)
        }
        return Mono.just(if (removed != null) 1L else 0L)
    }

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(data.containsKey(id))

    override fun <S : RecurrenceSeriesEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        data[id] = entity
        return Mono.just(entity)
    }

    override fun <S : RecurrenceSeriesEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<RecurrenceSeriesEntity> = Flux.fromIterable(id.mapNotNull { data[it] })

    private fun copySeries(
        current: RecurrenceSeriesEntity,
        qtyTotal: Int?,
    ): RecurrenceSeriesEntity =
        RecurrenceSeriesEntity(
            qtyTotal = qtyTotal,
        ).also {
            it.id = current.id
            it.createdAt = current.createdAt
            it.updatedAt = nowOffset()
        }
}
