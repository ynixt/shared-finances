package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryPath
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryScope
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

internal class InMemoryWalletEventRepository(
    private val walletItemRepository: InMemoryWalletItemRepository,
) : WalletEventRepository {
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

    override fun findAll(
        scope: WalletTransactionQueryScope,
        limit: Int,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): Flux<WalletEventEntity> {
        val filtered =
            data.values
                .asSequence()
                .filter { event ->
                    when (scope.path) {
                        WalletTransactionQueryPath.OWNERSHIP -> {
                            val hasOwnedItem =
                                event.entries
                                    .orEmpty()
                                    .any { entry ->
                                        walletItemRepository.getOrNull(entry.walletItemId)?.userId?.let(scope.ownerUserIds::contains) ==
                                            true
                                    }
                            val groupMatches = scope.groupIds.isEmpty() || (event.groupId != null && scope.groupIds.contains(event.groupId))
                            hasOwnedItem && groupMatches
                        }
                        WalletTransactionQueryPath.GROUP_SCOPE -> event.groupId != null && scope.groupIds.contains(event.groupId)
                    }
                }.filter { entryTypes.isEmpty() || entryTypes.contains(it.type) }
                .filter { minimumDate == null || !it.date.isBefore(minimumDate) }
                .filter { maximumDate == null || !it.date.isAfter(maximumDate) }
                .filter {
                    billId == null ||
                        it.entries
                            .orEmpty()
                            .filterIsInstance<WalletEntryEntity>()
                            .any { entry -> entry.billId == billId }
                }.filter { walletItemId == null || it.entries.orEmpty().any { entry -> entry.walletItemId == walletItemId } }
                .filter { walletItemIds.isEmpty() || it.entries.orEmpty().any { entry -> walletItemIds.contains(entry.walletItemId) } }
                .sortedWith(
                    compareByDescending<WalletEventEntity> { it.date }
                        .thenByDescending { it.id },
                ).filter { event ->
                    if (cursor == null) {
                        true
                    } else if (event.date.isBefore(cursor.maximumDate)) {
                        true
                    } else if (event.date != cursor.maximumDate) {
                        false
                    } else {
                        val eventId = event.id ?: return@filter false
                        eventId < cursor.maximumId
                    }
                }.take(limit)
                .toList()

        return Flux.fromIterable(filtered)
    }
}
