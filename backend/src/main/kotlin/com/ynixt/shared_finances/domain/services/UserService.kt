package com.ynixt.shared_finances.domain.services

import com.ynixt.shared_finances.domain.entities.User
import com.ynixt.shared_finances.domain.models.dto.CreateUserRequestDto
import reactor.core.publisher.Flux

interface UserService {
    suspend fun createUser(request: CreateUserRequestDto): User
    fun getAllUsers(): Flux<User>
}