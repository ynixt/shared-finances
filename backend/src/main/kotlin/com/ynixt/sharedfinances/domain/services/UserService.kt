package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.application.web.dto.kratos.CreateUserRequestDto
import com.ynixt.sharedfinances.domain.entities.User
import java.util.UUID

interface UserService {
    suspend fun createUser(request: CreateUserRequestDto): User

    suspend fun changeLanguage(
        userId: UUID,
        newLang: String,
    )

    suspend fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    )
}
