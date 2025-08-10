package com.ynixt.shared_finances.domain.repositories

import com.ynixt.shared_finances.domain.entities.User
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserRepository {
    fun save(user: User): Mono<User>
    fun findByEmail(email: String): Flux<User>
    fun findAll(): Flux<User>
    fun findByExternalId(externalId: String): Flux<User>
}