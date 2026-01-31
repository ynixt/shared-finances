package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.application.web.dto.user.UpdateUserDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.http.codec.multipart.FilePart
import java.util.UUID

interface UserService {
    suspend fun createUser(request: RegisterDto): UserEntity

    suspend fun changeLanguage(
        userId: UUID,
        newLang: String,
    )

    fun findAllByIdIn(ids: Collection<UUID>): Flow<UserEntity>

    suspend fun updateUser(
        userId: UUID,
        updateUserDto: UpdateUserDto,
        newAvatar: FilePart?,
    ): UserEntity

    suspend fun changePassword(
        userId: UUID,
        currentPasswordHash: String?,
        newPasswordHash: String,
    )
}
