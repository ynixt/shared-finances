package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface UserSpringDataRepository :
    Repository<UserEntity, String>,
    EntityRepository<UserEntity> {
    fun findOneByEmail(email: String): Mono<UserEntity>

    @Modifying
    @Query(
        """
        update users
        set lang = :newLang,
        updated_at = CURRENT_TIMESTAMP
        where id = :userId
    """,
    )
    fun changeLanguage(
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
    fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ): Mono<Int>

    @Modifying
    @Query(
        """
        update users
        set password_hash = :newPasswordHash,
        updated_at = CURRENT_TIMESTAMP
        where id = :userId
    """,
    )
    fun changePassword(
        userId: UUID,
        newPasswordHash: String,
    ): Mono<Int>

    @Query(
        """
        select u.*
        from group_user current_gu 
        join group g on g.id = current_gu.group_id
        join group_user gu on gu.group_id = g.id
        join users u on u.id = gu.user_id
        where
            current_gu.user_id = :userId 
    """,
    )
    fun findAllUsersInSameGroup(userId: UUID): Flux<UserEntity>

    @Modifying
    @Query(
        """
        update users
        set 
            totp_secret = :totpSecret,
            mfa_enabled = true,
            updated_at = CURRENT_TIMESTAMP
        where id = :userId
    """,
    )
    fun enableMfa(
        userId: UUID,
        totpSecret: String,
    ): Mono<Int>

    @Modifying
    @Query(
        """
        update users
        set 
            totp_secret = null,
            mfa_enabled = false,
            updated_at = CURRENT_TIMESTAMP
        where id = :userId
    """,
    )
    fun disableMfa(userId: UUID): Mono<Int>
}
