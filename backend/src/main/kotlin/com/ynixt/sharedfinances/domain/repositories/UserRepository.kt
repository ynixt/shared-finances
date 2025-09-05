package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.User
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface UserRepository {
    fun save(user: User): Mono<User>

    fun findByEmail(email: String): Flux<User>

    fun findAll(): Flux<User>

    fun findByExternalId(externalId: String): Flux<User>

    fun changeLanguage(
        userId: UUID,
        newLang: String,
    ): Mono<Int>

    fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ): Mono<Int>
}
