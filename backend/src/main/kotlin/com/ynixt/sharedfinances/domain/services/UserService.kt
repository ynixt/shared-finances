package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.application.web.dto.user.UpdateUserDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface UserService {
    suspend fun createUser(request: RegisterDto): UserEntity

    suspend fun changeLanguage(
        userId: UUID,
        newLang: String,
    )

    suspend fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    )

    fun findAllByIdIn(ids: Collection<UUID>): Flux<UserEntity>

    fun updateUser(
        userId: UUID,
        updateUserDto: UpdateUserDto,
        newAvatar: FilePart?,
    ): Mono<UserEntity>

    fun changePassword(
        userId: UUID,
        currentPasswordHash: String?,
        newPasswordHash: String,
    ): Mono<Void>
}
