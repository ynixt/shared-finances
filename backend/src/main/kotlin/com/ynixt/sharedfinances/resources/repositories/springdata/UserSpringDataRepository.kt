package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.User
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface UserSpringDataRepository :
    UserRepository,
    ReactiveCrudRepository<User, String> {
    override fun findByEmail(email: String): Flux<User>

    override fun findByExternalId(externalId: String): Flux<User>

    @Modifying
    @Query(
        """
        update users
        set lang = :newLang,
        updated_at = CURRENT_TIMESTAMP
        where id = :userId
    """,
    )
    override fun changeLanguage(
        userId: UUID,
        newLang: String,
    ): Mono<Int>

    @Modifying
    @Query(
        """
        update users
        set default_currency = :newDefaultCurrency,
        updated_at = CURRENT_TIMESTAMP
        where id = :userId
    """,
    )
    override fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ): Mono<Int>
}
