package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.UserEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface UserRepository {
    fun save(user: UserEntity): Mono<UserEntity>

    fun findByEmail(email: String): Flux<UserEntity>

    fun findAll(): Flux<UserEntity>

    fun findByExternalId(externalId: String): Flux<UserEntity>

    fun changeLanguage(
        userId: UUID,
        newLang: String,
    ): Mono<Int>

    fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ): Mono<Int>
}
