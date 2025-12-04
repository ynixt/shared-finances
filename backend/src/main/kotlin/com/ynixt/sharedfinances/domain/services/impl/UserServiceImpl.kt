package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.application.web.dto.kratos.CreateUserRequestDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.UserService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserServiceImpl(
    override val repository: UserRepository,
) : EntityServiceImpl<UserEntity, UserEntity>(),
    UserService {
    @Transactional
    override suspend fun createUser(request: CreateUserRequestDto): UserEntity {
        val user =
            UserEntity(
                externalId = request.uid,
                email = request.email,
                firstName = request.firstName,
                lastName = request.lastName,
                lang = request.lang,
                defaultCurrency = request.defaultCurrency,
            )

        return repository.save(user).awaitSingle()
    }

    @Transactional
    override suspend fun changeLanguage(
        userId: UUID,
        newLang: String,
    ) {
        repository.changeLanguage(userId, newLang).awaitSingle()
    }

    @Transactional
    override suspend fun changeDefaultCurrency(
        userId: UUID,
        newDefaultCurrency: String,
    ) {
        repository.changeDefaultCurrency(userId, newDefaultCurrency).awaitSingle()
    }
}
