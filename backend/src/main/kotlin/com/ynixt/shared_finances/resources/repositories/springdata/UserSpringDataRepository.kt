package com.ynixt.shared_finances.resources.repositories.springdata

import com.ynixt.shared_finances.domain.entities.User
import com.ynixt.shared_finances.domain.repositories.UserRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface UserSpringDataRepository : UserRepository, ReactiveCrudRepository<User, String> {
    override fun findByEmail(email: String): Flux<User>
    override fun findByExternalId(externalId: String): Flux<User>
}