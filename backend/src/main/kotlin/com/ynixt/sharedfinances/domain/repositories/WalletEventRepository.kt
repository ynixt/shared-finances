package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

data class WalletEventCursorFindAll(
    val maximumId: UUID,
    val maximumDate: LocalDate,
)

interface WalletEventRepository {
    fun findById(id: UUID): Mono<WalletEventEntity>

    fun deleteById(id: UUID): Mono<Long>

    fun findOneByRecurrenceEventIdAndDate(
        recurrenceEventId: UUID,
        date: LocalDate,
    ): Mono<WalletEventEntity>

    fun findAllByRecurrenceEventId(recurrenceEventId: UUID): Flux<WalletEventEntity>

    fun save(walletEntry: WalletEventEntity): Mono<WalletEventEntity>

    fun saveAll(walletEntry: Iterable<WalletEventEntity>): Flux<WalletEventEntity>

    fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long>

    fun deleteAllByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long>

    /**
     * Deletes all wallet events authored by the user or backed by lines on wallet items they own,
     * including group-visible transactions, so CASCADE removes dependent wallet_entry rows before wallet_item is deleted.
     */
    fun deleteAllForAccountDeletion(userId: UUID): Mono<Long>

    fun findAll(
        scope: WalletTransactionQueryScope,
        limit: Int,
        walletItemId: UUID?,
        walletItemIds: Set<UUID> = emptySet(),
        entryTypes: Set<WalletEntryType> = emptySet(),
        categoryConceptIds: Set<UUID> = emptySet(),
        includeUncategorized: Boolean = false,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): Flux<WalletEventEntity>
}
