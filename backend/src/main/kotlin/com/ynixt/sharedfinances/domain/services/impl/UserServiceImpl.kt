package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.application.web.dto.kratos.CreateUserRequestDto
import com.ynixt.sharedfinances.domain.entities.User
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.UserService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
) : UserService {
    @Transactional
    override suspend fun createUser(request: CreateUserRequestDto): User {
        val user =
            User(
                externalId = request.uid,
                email = request.email,
                firstName = request.firstName,
                lastName = request.lastName,
                lang = request.lang,
                defaultCurrency = request.defaultCurrency,
            )

        return userRepository.save(user).awaitSingle()
    }

    @Transactional
    override suspend fun changeLanguage(
        userId: UUID,
        newLang: String,
    ) {
        userRepository.changeLanguage(userId, newLang).awaitSingle()
    }

    @Transactional
    override suspend fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ) {
        userRepository.changeDefaultCurrency(userId, newDefaultCurrency).awaitSingle()
    }
}
