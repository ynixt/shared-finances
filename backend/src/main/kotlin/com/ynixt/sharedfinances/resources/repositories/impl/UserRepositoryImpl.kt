package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.entitytemplate.UserEntityTemplateRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.UserSpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class UserRepositoryImpl(
    userSpringDataRepository: UserSpringDataRepository,
    private val userEntityTemplateRepository: UserEntityTemplateRepository,
) : EntityRepositoryImpl<UserSpringDataRepository, UserEntity>(userSpringDataRepository),
    UserRepository {
    override fun findOneByEmail(email: String): Mono<UserEntity> = springDataRepository.findOneByEmail(email)

    override fun changeLanguage(
        userId: UUID,
        newLang: String,
    ): Mono<Int> = springDataRepository.changeLanguage(userId, newLang)

    override fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ): Mono<Int> = springDataRepository.changeDefaultCurrency(userId, newDefaultCurrency)

    override fun changePassword(
        userId: UUID,
        newPasswordHash: String,
    ): Mono<Int> = springDataRepository.changePassword(userId, newPasswordHash)

    override fun changeOnboardingDone(
        userId: UUID,
        newOnboardingDone: Boolean,
    ): Mono<Int> = springDataRepository.changeOnboardingDone(userId, newOnboardingDone)

    override fun findAllUsersInSameGroup(userId: UUID): Flux<UserEntity> = springDataRepository.findAllUsersInSameGroup(userId)

    override fun enableMfa(
        userId: UUID,
        totpSecret: String,
    ): Mono<Int> = springDataRepository.enableMfa(userId, totpSecret)

    override fun disableMfa(userId: UUID): Mono<Int> = springDataRepository.disableMfa(userId)

    override fun insert(user: UserEntity): Mono<UserEntity> = userEntityTemplateRepository.insert(user)
}
