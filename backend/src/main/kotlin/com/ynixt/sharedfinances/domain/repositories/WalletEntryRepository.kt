package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface WalletEntryRepository {
    fun save(walletEntry: WalletEntryEntity): Mono<WalletEntryEntity>

    fun saveAll(walletEntry: Iterable<WalletEntryEntity>): Flux<WalletEntryEntity>
}
