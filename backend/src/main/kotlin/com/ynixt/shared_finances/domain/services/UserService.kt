package com.ynixt.shared_finances.domain.services

import com.ynixt.shared_finances.domain.entities.User
import com.ynixt.shared_finances.domain.models.dto.CreateUserRequestDto
import reactor.core.publisher.Flux
import java.util.*

interface UserService {
    suspend fun createUser(request: CreateUserRequestDto): User
    suspend fun changeLanguage(userId: UUID, newLang: String)
    fun getAllUsers(): Flux<User>
}