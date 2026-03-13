package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import org.springframework.data.repository.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface WalletEventSpringDataRepository : Repository<WalletEventEntity, String> {
    fun save(walletEntry: WalletEventEntity): Mono<WalletEventEntity>

    fun saveAll(walletEntry: Iterable<WalletEventEntity>): Flux<WalletEventEntity>
}
