package com.ynixt.sharedfinances.scenario.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.scenario.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

internal class InMemoryWalletEntryRepository : WalletEntryRepository {
    private val data = linkedMapOf<UUID, WalletEntryEntity>()

    override fun sumForBankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate,
        maximumDate: LocalDate?,
    ): Flux<EntrySumResult> = Flux.empty()

    override fun findById(id: UUID): Mono<WalletEntryEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(if (data.remove(id) != null) 1L else 0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(data.containsKey(id))

    override fun <S : WalletEntryEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        data[id] = entity
        return Mono.just(entity)
    }

    override fun <S : WalletEntryEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<WalletEntryEntity> = Flux.fromIterable(id.mapNotNull { data[it] })
}
