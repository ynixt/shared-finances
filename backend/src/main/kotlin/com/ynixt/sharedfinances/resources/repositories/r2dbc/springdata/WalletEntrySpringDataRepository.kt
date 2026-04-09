package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntrySpringDataRepository :
    EntityRepository<WalletEntryEntity>,
    R2dbcRepository<WalletEntryEntity, String> {
    fun findAllByWalletEventId(walletEventId: UUID): Flux<WalletEntryEntity>

    @Modifying
    fun deleteAllByWalletEventId(walletEventId: UUID): Mono<Int>
}
