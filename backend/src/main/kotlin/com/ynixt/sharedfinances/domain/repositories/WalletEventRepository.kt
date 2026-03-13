package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

data class WalletEventCursorFindAll(
    val maximumId: UUID,
    val maximumDate: LocalDate,
)

interface WalletEventRepository {
    fun save(walletEntry: WalletEventEntity): Mono<WalletEventEntity>

    fun saveAll(walletEntry: Iterable<WalletEventEntity>): Flux<WalletEventEntity>

    fun findAll(
        userId: UUID?,
        groupId: UUID?,
        limit: Int,
        walletItemId: UUID?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): Flux<WalletEventEntity>
}
