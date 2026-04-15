package com.ynixt.sharedfinances.scenarios.accountdeletion.support

import com.ynixt.sharedfinances.domain.entities.SessionEntity
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

internal object NoOpSessionRepository : SessionRepository {
    override fun deleteAllByUserId(userId: UUID): Mono<Long> = Mono.just(0L)

    override fun findById(id: UUID): Mono<SessionEntity> = Mono.empty()

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(false)

    override fun <S : SessionEntity> save(entity: S): Mono<S> = Mono.just(entity)

    override fun <S : SessionEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity)

    override fun findAllByIdIn(id: Collection<UUID>): Flux<SessionEntity> = Flux.empty()
}
