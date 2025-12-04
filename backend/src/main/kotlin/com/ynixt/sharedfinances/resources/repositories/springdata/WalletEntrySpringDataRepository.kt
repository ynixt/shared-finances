package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import org.springframework.data.repository.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface WalletEntrySpringDataRepository : Repository<WalletEntryEntity, String> {
    fun save(walletEntry: WalletEntryEntity): Mono<WalletEntryEntity>

    fun saveAll(walletEntry: Iterable<WalletEntryEntity>): Flux<WalletEntryEntity>
}
