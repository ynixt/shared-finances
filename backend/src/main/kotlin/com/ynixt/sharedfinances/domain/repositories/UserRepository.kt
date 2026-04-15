package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.UserEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface UserRepository : EntityRepository<UserEntity> {
    fun findDistinctDefaultCurrencies(): Flux<String>

    fun findOneByEmail(email: String): Mono<UserEntity>

    fun changeLanguage(
        userId: UUID,
        newLang: String,
    ): Mono<Int>

    fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ): Mono<Int>

    fun changePassword(
        userId: UUID,
        newPasswordHash: String,
    ): Mono<Int>

    fun enableMfa(
        userId: UUID,
        totpSecret: String,
    ): Mono<Int>

    fun disableMfa(userId: UUID): Mono<Int>

    fun findAllUsersInSameGroup(userId: UUID): Flux<UserEntity>

    fun insert(user: UserEntity): Mono<UserEntity>

    fun changeOnboardingDone(
        userId: UUID,
        newOnboardingDone: Boolean,
    ): Mono<Int>

    fun findUnverifiedUserIdsCreatedBefore(cutoff: OffsetDateTime): Flux<UUID>

    fun markEmailVerifiedIfUnverified(userId: UUID): Mono<Int>

    fun updateEmailWhenUnverified(
        userId: UUID,
        newEmail: String,
    ): Mono<Int>
}
