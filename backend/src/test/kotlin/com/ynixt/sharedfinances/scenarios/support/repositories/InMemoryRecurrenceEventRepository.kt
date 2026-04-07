package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

internal class InMemoryRecurrenceEventRepository : RecurrenceEventRepository {
    private val data = linkedMapOf<UUID, RecurrenceEventEntity>()

    override fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<RecurrenceEventEntity> =
        Flux.fromIterable(
            data.values.filter {
                it.nextExecution != null && (it.nextExecution.isBefore(nextExecution) || it.nextExecution == nextExecution)
            },
        )

    override fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long> {
        val initial = data.size
        data.entries.removeIf { (_, value) -> value.userId == userId }
        return Mono.just((initial - data.size).toLong())
    }

    override fun updateConfigCausedByExecution(
        id: UUID,
        oldNextExecution: LocalDate,
        nextExecution: LocalDate?,
    ): Mono<Int> {
        val current = data[id] ?: return Mono.just(0)
        if (current.nextExecution != oldNextExecution) {
            return Mono.just(0)
        }

        data[id] =
            copyRecurrence(
                current = current,
                qtyExecuted = current.qtyExecuted + 1,
                lastExecution = oldNextExecution,
                nextExecution = nextExecution,
            )
        return Mono.just(1)
    }

    override fun findAll(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        userId: UUID?,
        groupId: UUID?,
        sort: Sort,
    ): Flux<RecurrenceEventEntity> {
        val filtered =
            data.values
                .asSequence()
                .filter { userId == null || it.userId == userId }
                .filter { groupId == null || it.groupId == groupId }
                .filter { maximumNextExecution == null || (it.nextExecution != null && !it.nextExecution.isAfter(maximumNextExecution)) }
                .filter { minimumEndExecution == null || (it.endExecution != null && !it.endExecution.isBefore(minimumEndExecution)) }
                .toList()
        return Flux.fromIterable(filtered)
    }

    override fun findById(id: UUID): Mono<RecurrenceEventEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(if (data.remove(id) != null) 1L else 0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(data.containsKey(id))

    override fun <S : RecurrenceEventEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        data[id] = entity
        return Mono.just(entity)
    }

    override fun <S : RecurrenceEventEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<RecurrenceEventEntity> = Flux.fromIterable(id.mapNotNull { data[it] })

    private fun copyRecurrence(
        current: RecurrenceEventEntity,
        qtyExecuted: Int = current.qtyExecuted,
        lastExecution: LocalDate? = current.lastExecution,
        nextExecution: LocalDate? = current.nextExecution,
    ): RecurrenceEventEntity =
        RecurrenceEventEntity(
            name = current.name,
            categoryId = current.categoryId,
            userId = current.userId,
            groupId = current.groupId,
            tags = current.tags,
            observations = current.observations,
            type = current.type,
            periodicity = current.periodicity,
            paymentType = current.paymentType,
            qtyExecuted = qtyExecuted,
            qtyLimit = current.qtyLimit,
            lastExecution = lastExecution,
            nextExecution = nextExecution,
            endExecution = current.endExecution,
        ).also {
            it.id = current.id
            it.entries = current.entries
            it.createdAt = current.createdAt
            it.updatedAt = nowOffset()
        }
}
