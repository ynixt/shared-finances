package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

data class WalletEntryCursorFindAll(
    val maximumId: UUID,
    val maximumDate: LocalDate,
)

interface WalletEntryRepository {
    fun save(walletEntry: WalletEntryEntity): Mono<WalletEntryEntity>

    fun saveAll(walletEntry: Iterable<WalletEntryEntity>): Flux<WalletEntryEntity>

    fun findAll(
        userId: UUID?,
        groupId: UUID?,
        limit: Int,
        walletItemId: UUID?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        cursor: WalletEntryCursorFindAll?,
    ): Flux<WalletEntryEntity>

    fun sumForBankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate,
        maximumDate: LocalDate?,
    ): Flux<EntrySumResult>
}
