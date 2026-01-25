package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.UserR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.UserSpringDataRepository
import io.lettuce.core.KillArgs.Builder.id
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class UserRepositoryImpl(
    private val userSpringDataRepository: UserSpringDataRepository,
    private val userR2DBCRepository: UserR2DBCRepository,
) : UserRepository {
    override fun findOneByEmail(email: String): Mono<UserEntity> = userSpringDataRepository.findOneByEmail(email)

    override fun changeLanguage(
        userId: UUID,
        newLang: String,
    ): Mono<Int> = userSpringDataRepository.changeLanguage(userId, newLang)

    override fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ): Mono<Int> = userSpringDataRepository.changeDefaultCurrency(userId, newDefaultCurrency)

    override fun changePassword(
        userId: UUID,
        newPasswordHash: String,
    ): Mono<Int> = userSpringDataRepository.changePassword(userId, newPasswordHash)

    override fun findAllUsersInSameGroup(userId: UUID): Flux<UserEntity> = userSpringDataRepository.findAllUsersInSameGroup(userId)

    override fun insert(user: UserEntity): Mono<UserEntity> = userR2DBCRepository.insert(user)

    override fun findById(id: UUID): Mono<UserEntity> = userSpringDataRepository.findById(id)

    override fun deleteById(id: UUID): Mono<Long> = userSpringDataRepository.deleteById(id)

    override fun existsById(id: UUID): Mono<Boolean> = userSpringDataRepository.existsById(id)

    override fun save(entity: UserEntity): Mono<UserEntity> = userSpringDataRepository.save(entity)

    override fun saveAll(entity: Iterable<UserEntity>): Flux<UserEntity> = userSpringDataRepository.saveAll(entity)

    override fun findAllByIdIn(id: Collection<UUID>): Flux<UserEntity> = userSpringDataRepository.findAllByIdIn(id)

    override fun enableMfa(
        userId: UUID,
        totpSecret: String,
    ): Mono<Int> = userSpringDataRepository.enableMfa(userId, totpSecret)

    override fun disableMfa(userId: UUID): Mono<Int> = userSpringDataRepository.disableMfa(userId)
}
