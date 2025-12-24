package com.ynixt.sharedfinances.domain.services.impl

import com.fasterxml.uuid.Generators
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.exceptions.EmailAlreadyInUseException
import com.ynixt.sharedfinances.domain.models.Wrapper
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.UserService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.Locale.getDefault
import java.util.UUID

@Service
class UserServiceImpl(
    override val repository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseHelperService: DatabaseHelperService,
    private val avatarService: AvatarService,
) : EntityServiceImpl<UserEntity, UserEntity>(),
    UserService {
    @Transactional
    override suspend fun createUser(request: RegisterDto): UserEntity {
        val user =
            UserEntity(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                firstName = request.firstName,
                lastName = request.lastName,
                lang = request.lang,
                defaultCurrency = request.defaultCurrency,
                tmz = request.tmz,
                photoUrl = null,
                emailVerified = false,
                mfaEnabled = false,
                totpSecret = null,
            ).also {
                it.id = Generators.timeBasedEpochRandomGenerator().generate()
            }

        return avatarService
            .getPhotoFromGravatar(user.email, user.id!!)
            .map { Wrapper(it) }
            .onErrorReturn(Wrapper(null))
            .defaultIfEmpty(Wrapper(null))
            .flatMap { avatarUrlWrapper ->
                user.photoUrl = avatarUrlWrapper.value

                repository
                    .insert(user)
                    .onErrorMap { t ->
                        if (databaseHelperService.isUniqueViolation(t, "users_email_key")) {
                            EmailAlreadyInUseException(request.email)
                        } else {
                            t
                        }
                    }
            }.awaitSingle()
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

    private fun getPhotoFromGravatar(email: String) {
        val hash = MessageDigest.getInstance("MD5").digest(email.trim().lowercase(getDefault()).toByteArray())
        val size = 256
        val rating = "g"

        val url = "https://www.gravatar.com/avatar/$hash?s=$size&d=404}&r=$rating"

        // TODO: baixar
        // TODO fazer upload no minion
    }
}
