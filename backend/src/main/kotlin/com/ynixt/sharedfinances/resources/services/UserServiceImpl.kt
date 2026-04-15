package com.ynixt.sharedfinances.resources.services

import com.fasterxml.uuid.Generators
import com.ynixt.sharedfinances.application.config.LegalDocumentProperties
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.application.web.dto.user.UpdateUserDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.exceptions.http.EmailAlreadyInUseException
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.UserService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.annotation.Lazy
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class UserServiceImpl(
    override val repository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseHelperService: DatabaseHelperService,
    private val avatarService: AvatarService,
    private val legalDocumentProperties: LegalDocumentProperties,
    private val clock: Clock,
    @Lazy private val accountDeletionService: AccountDeletionService,
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

        val acceptedAt = OffsetDateTime.ofInstant(clock.instant(), clock.zone)
        user.termsAcceptedAt = acceptedAt
        user.termsVersion = legalDocumentProperties.termsVersion
        user.privacyAcceptedAt = acceptedAt
        user.privacyVersion = legalDocumentProperties.privacyVersion

        if (request.gravatarOptIn) {
            user.photoUrl = avatarService.getPhotoFromGravatar(user.email, user.id!!)
        }

        return repository
            .insert(user)
            .onErrorMap { t ->
                if (databaseHelperService.isUniqueViolation(t, "users_email_key")) {
                    EmailAlreadyInUseException(request.email)
                } else {
                    t
                }
            }.awaitSingle()
    }

    override suspend fun changePassword(
        userId: UUID,
        currentPasswordHash: String?,
        newPasswordHash: String,
    ) {
        repository
            .findById(userId)
            .awaitSingleOrNull()
            ?.let { user ->
                val correctHash = user.passwordHash

                if (correctHash != null && !passwordEncoder.matches(currentPasswordHash, correctHash)) {
                    throw BadCredentialsException("invalid credentials")
                }

                repository
                    .changePassword(
                        userId = userId,
                        newPasswordHash = passwordEncoder.encode(newPasswordHash)!!,
                    ).awaitSingle()
                    .let { if (it == 0) null else user }
            } ?: throw BadCredentialsException("invalid credentials")
    }

    @Transactional
    override suspend fun changeLanguage(
        userId: UUID,
        newLang: String,
    ) {
        repository.changeLanguage(userId, newLang).awaitSingle()
    }

    override suspend fun updateUser(
        userId: UUID,
        updateUserDto: UpdateUserDto,
        newAvatar: FilePart?,
    ): UserEntity =
        repository
            .findById(userId)
            .awaitSingle()
            .let { user ->
                user.email = updateUserDto.email
                user.firstName = updateUserDto.firstName
                user.lastName = updateUserDto.lastName
                user.lang = updateUserDto.lang
                user.defaultCurrency = updateUserDto.defaultCurrency
                user.tmz = updateUserDto.tmz

                if (user.photoUrl != null && (updateUserDto.removeAvatar || updateUserDto.getFromGravatar)) {
                    avatarService.deletePhoto(userId)
                }

                if (updateUserDto.removeAvatar) {
                    user.photoUrl = null
                } else if (updateUserDto.getFromGravatar) {
                    avatarService.getPhotoFromGravatar(user.email, userId).let {
                        user.photoUrl = it
                        it
                    }
                } else if (newAvatar != null) {
                    avatarService
                        .upload(
                            userId = userId,
                            file = newAvatar,
                        ).let {
                            user.photoUrl = it
                            it
                        }
                }

                repository.save(user).awaitSingle()
            }

    override suspend fun deleteCurrentAccount(userId: UUID) {
        accountDeletionService.deleteAccountForUser(userId)
    }
}
