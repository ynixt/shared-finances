package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.FileStorageService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import reactor.core.publisher.Flux
import java.util.UUID

class AvatarReadServiceImplTest {
    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val fileStorage = RecordingFileStorage()
    private val service = AvatarReadServiceImpl(userRepository, fileStorage)

    @Test
    fun `owner can read the avatar`() =
        runTest {
            val ownerId = UUID.randomUUID()
            fileStorage.resource = ByteArrayResource(byteArrayOf(1))

            assertNotNull(service.getAvatar(ownerId, ownerId))
        }

    @Test
    fun `user in the same group can read the avatar`() =
        runTest {
            val ownerId = UUID.randomUUID()
            val loggedUserId = UUID.randomUUID()
            fileStorage.resource = ByteArrayResource(byteArrayOf(1))
            Mockito
                .`when`(userRepository.findAllUsersInSameGroup(loggedUserId))
                .thenReturn(Flux.just(user(ownerId)))

            assertNotNull(service.getAvatar(ownerId, loggedUserId))
        }

    @Test
    fun `unrelated user cannot read the avatar`() =
        runTest {
            val ownerId = UUID.randomUUID()
            val loggedUserId = UUID.randomUUID()
            fileStorage.resource = ByteArrayResource(byteArrayOf(1))
            Mockito
                .`when`(userRepository.findAllUsersInSameGroup(loggedUserId))
                .thenReturn(Flux.empty())

            assertNull(service.getAvatar(ownerId, loggedUserId))
            assertNull(fileStorage.lastReadKey)
        }

    @Test
    fun `authorized user receives null when the file is absent`() =
        runTest {
            val ownerId = UUID.randomUUID()

            assertNull(service.getAvatar(ownerId, ownerId))
        }

    private fun user(id: UUID): UserEntity =
        UserEntity(
            email = "user@example.com",
            passwordHash = null,
            firstName = "Test",
            lastName = "User",
            lang = "en-US",
            defaultCurrency = "USD",
            tmz = "UTC",
            photoUrl = null,
            emailVerified = true,
            mfaEnabled = false,
            totpSecret = null,
            onboardingDone = true,
        ).also { it.id = id }

    private class RecordingFileStorage : FileStorageService {
        var resource: Resource? = null
        var lastReadKey: String? = null

        override suspend fun write(
            key: String,
            bytes: ByteArray,
        ) = Unit

        override suspend fun find(key: String): Resource? {
            lastReadKey = key
            return resource
        }

        override suspend fun delete(key: String): Boolean = false
    }
}
