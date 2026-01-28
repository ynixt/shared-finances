package com.ynixt.sharedfinances.domain.services.impl

import com.fasterxml.uuid.Generators
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.application.web.dto.user.UpdateUserDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.exceptions.http.EmailAlreadyInUseException
import com.ynixt.sharedfinances.domain.models.Wrapper
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.UserService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
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
                onboardingDone = false,
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

    override fun changePassword(
        userId: UUID,
        currentPasswordHash: String?,
        newPasswordHash: String,
    ): Mono<Void> {
        return repository
            .findById(userId)
            .switchIfEmpty(Mono.error(BadCredentialsException("invalid credentials")))
            .flatMap { user ->
                val correctHash = user.passwordHash

                if (correctHash != null && !passwordEncoder.matches(currentPasswordHash, correctHash)) {
                    return@flatMap Mono.error(BadCredentialsException("invalid credentials"))
                }

                repository
                    .changePassword(
                        userId = userId,
                        newPasswordHash = passwordEncoder.encode(newPasswordHash)!!,
                    ).then()
            }
    }

    @Transactional
    override suspend fun changeLanguage(
        userId: UUID,
        newLang: String,
    ) {
        repository.changeLanguage(userId, newLang).awaitSingle()
    }

    override fun updateUser(
        userId: UUID,
        updateUserDto: UpdateUserDto,
        newAvatar: FilePart?,
    ): Mono<UserEntity> =
        repository
            .findById(userId)
            .flatMap { user ->
                user.email = updateUserDto.email
                user.firstName = updateUserDto.firstName
                user.lastName = updateUserDto.lastName
                user.lang = updateUserDto.lang
                user.defaultCurrency = updateUserDto.defaultCurrency
                user.tmz = updateUserDto.tmz

                val removePhotoMono =
                    if (user.photoUrl != null && (updateUserDto.removeAvatar || updateUserDto.getFromGravatar)) {
                        avatarService.deletePhoto(userId).then()
                    } else {
                        Mono.empty()
                    }
                var newPhotoMono = Mono.empty<String>()

                if (updateUserDto.removeAvatar) {
                    user.photoUrl = null
                } else if (updateUserDto.getFromGravatar) {
                    newPhotoMono =
                        avatarService.getPhotoFromGravatar(user.email, userId).map {
                            user.photoUrl = it
                            it
                        }
                } else if (newAvatar != null) {
                    newPhotoMono =
                        avatarService
                            .upload(
                                userId = userId,
                                file = newAvatar,
                            ).map {
                                user.photoUrl = it
                                it
                            }
                }

                removePhotoMono
                    .then(newPhotoMono)
                    .then(repository.save(user))
            }.flatMap {
                repository.findById(userId)
            }
}
