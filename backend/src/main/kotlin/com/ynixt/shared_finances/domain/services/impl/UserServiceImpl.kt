package com.ynixt.shared_finances.domain.services.impl

import com.ynixt.shared_finances.domain.entities.User
import com.ynixt.shared_finances.domain.models.dto.CreateUserRequestDto
import com.ynixt.shared_finances.domain.repositories.UserRepository
import com.ynixt.shared_finances.domain.services.UserService
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import java.util.*

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
) : UserService {

    @Transactional
    override suspend fun createUser(request: CreateUserRequestDto): User {
        val user = User(
            externalId = request.uid,
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            lang = request.lang,
        )

        return userRepository.save(user).awaitSingle()
    }

    @Transactional
    override suspend fun changeLanguage(userId: UUID, newLang: String) {
        userRepository.changeLanguage(userId, newLang).awaitSingle()
    }

    override fun getAllUsers(): Flux<User> = userRepository.findAll()
}