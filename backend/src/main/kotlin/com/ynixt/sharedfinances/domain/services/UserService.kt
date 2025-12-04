package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.application.web.dto.kratos.CreateUserRequestDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import reactor.core.publisher.Flux
import java.util.UUID

interface UserService {
    suspend fun createUser(request: CreateUserRequestDto): UserEntity

    suspend fun changeLanguage(
        userId: UUID,
        newLang: String,
    )

    suspend fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    )

    fun findAllByIdIn(ids: Collection<UUID>): Flux<UserEntity>
}
