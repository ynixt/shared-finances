package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryPath
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryScope
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

internal class InMemoryRecurrenceEventRepository(
    private val walletItemRepository: InMemoryWalletItemRepository,
) : RecurrenceEventRepository {
    private val data = linkedMapOf<UUID, RecurrenceEventEntity>()

    fun findIdsBySeriesId(seriesId: UUID): Set<UUID> =
        data.values
            .filter { it.seriesId == seriesId }
            .mapNotNull { it.id }
            .toSet()

    fun deleteAllBySeriesId(seriesId: UUID): Long {
        val initial = data.size
        data.entries.removeIf { (_, value) -> value.seriesId == seriesId }
        return (initial - data.size).toLong()
    }

    override fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<RecurrenceEventEntity> =
        Flux.fromIterable(
            data.values.filter {
                it.nextExecution != null && (it.nextExecution.isBefore(nextExecution) || it.nextExecution == nextExecution)
            },
        )

    override fun findAllBySeriesId(seriesId: UUID): Flux<RecurrenceEventEntity> =
        Flux.fromIterable(data.values.filter { it.seriesId == seriesId })

    override fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long> {
        val initial = data.size
        data.entries.removeIf { (_, value) ->
            value.entries
                .orEmpty()
                .any { entry ->
                    entry.walletItemId == walletItemId &&
                        walletItemRepository.getOrNull(entry.walletItemId)?.userId == userId
                }
        }
        return Mono.just((initial - data.size).toLong())
    }

    override fun deleteAllByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long> {
        val initial = data.size
        data.entries.removeIf { (_, value) -> value.groupId == groupId && value.createdByUserId == userId }
        return Mono.just((initial - data.size).toLong())
    }

    override fun deleteAllForAccountDeletion(userId: UUID): Mono<Long> {
        val initial = data.size
        data.entries.removeIf { (_, value) ->
            value.createdByUserId == userId ||
                value.entries
                    .orEmpty()
                    .any { entry -> walletItemRepository.getOrNull(entry.walletItemId)?.userId == userId }
        }
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

    override fun findAllEntries(
        scope: WalletTransactionQueryScope,
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        sort: Sort,
    ): Flux<RecurrenceEventEntity> {
        val filtered =
            data.values
                .asSequence()
                .filter { maximumNextExecution == null || (it.nextExecution != null && !it.nextExecution.isAfter(maximumNextExecution)) }
                .filter { minimumEndExecution == null || (it.endExecution == null || !it.endExecution.isBefore(minimumEndExecution)) }
                .filter { entryTypes.isEmpty() || entryTypes.contains(it.type) }
                .filter { event ->
                    when (scope.path) {
                        WalletTransactionQueryPath.OWNERSHIP -> {
                            val ownedEntries =
                                event.entries
                                    .orEmpty()
                                    .any { entry ->
                                        walletItemRepository.getOrNull(entry.walletItemId)?.userId?.let(scope.ownerUserIds::contains) ==
                                            true
                                    }
                            val groupMatches = scope.groupIds.isEmpty() || (event.groupId != null && scope.groupIds.contains(event.groupId))
                            ownedEntries && groupMatches
                        }
                        WalletTransactionQueryPath.GROUP_SCOPE -> event.groupId != null && scope.groupIds.contains(event.groupId)
                    }
                }.filter { event ->
                    val entries = event.entries.orEmpty().filterIsInstance<RecurrenceEntryEntity>()
                    val walletItemMatches =
                        walletItemId == null ||
                            entries.any { entry ->
                                entry.walletItemId == walletItemId &&
                                    (
                                        billDate == null ||
                                            entry.nextBillDate == null ||
                                            (
                                                !entry.nextBillDate.isAfter(billDate) &&
                                                    (entry.lastBillDate == null || !entry.lastBillDate.isBefore(billDate))
                                            )
                                    )
                            }

                    val walletItemIdsMatches =
                        walletItemIds.isEmpty() ||
                            entries.any { entry -> walletItemIds.contains(entry.walletItemId) }

                    walletItemMatches && walletItemIdsMatches
                }.toList()
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
            createdByUserId = current.createdByUserId,
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
            seriesId = current.seriesId,
            seriesOffset = current.seriesOffset,
        ).also {
            it.id = current.id
            it.entries = current.entries
            it.createdAt = current.createdAt
            it.updatedAt = nowOffset()
            it.seriesQtyTotal = current.seriesQtyTotal
        }
}
