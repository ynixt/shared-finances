package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

internal class InMemoryUserRepository : UserRepository {
    private val data = linkedMapOf<UUID, UserEntity>()

    override fun findDistinctDefaultCurrencies(): Flux<String> = Flux.fromIterable(data.values.map { it.defaultCurrency }.distinct())

    override fun findOneByEmail(email: String): Mono<UserEntity> = Mono.justOrEmpty(data.values.firstOrNull { it.email == email })

    override fun changeLanguage(
        userId: UUID,
        newLang: String,
    ): Mono<Int> =
        Mono.just(
            data[userId]?.let {
                it.lang = newLang
                1
            } ?: 0,
        )

    override fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ): Mono<Int> =
        Mono.just(
            data[userId]?.let {
                it.defaultCurrency = newDefaultCurrency
                1
            } ?: 0,
        )

    override fun changePassword(
        userId: UUID,
        newPasswordHash: String,
    ): Mono<Int> =
        Mono.just(
            data[userId]?.let {
                it.passwordHash = newPasswordHash
                1
            } ?: 0,
        )

    override fun enableMfa(
        userId: UUID,
        totpSecret: String,
    ): Mono<Int> =
        Mono.just(
            data[userId]?.let {
                it.mfaEnabled = true
                it.totpSecret = totpSecret
                1
            } ?: 0,
        )

    override fun disableMfa(userId: UUID): Mono<Int> =
        Mono.just(
            data[userId]?.let {
                it.mfaEnabled = false
                it.totpSecret = null
                1
            } ?: 0,
        )

    override fun findAllUsersInSameGroup(userId: UUID): Flux<UserEntity> = Flux.empty()

    override fun insert(user: UserEntity): Mono<UserEntity> {
        val id = user.id ?: UUID.randomUUID()
        user.id = id
        user.createdAt = user.createdAt ?: nowOffset()
        user.updatedAt = nowOffset()
        data[id] = user
        return Mono.just(user)
    }

    override fun changeOnboardingDone(
        userId: UUID,
        newOnboardingDone: Boolean,
    ): Mono<Int> =
        Mono.just(
            data[userId]?.let {
                it.onboardingDone = newOnboardingDone
                1
            } ?: 0,
        )

    override fun findById(id: UUID): Mono<UserEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(if (data.remove(id) != null) 1L else 0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(data.containsKey(id))

    override fun <S : UserEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        data[id] = entity
        return Mono.just(entity)
    }

    override fun <S : UserEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<UserEntity> = Flux.fromIterable(id.mapNotNull { data[it] })
}
