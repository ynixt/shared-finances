package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AvatarReadService
import com.ynixt.sharedfinances.domain.services.FileStorageService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AvatarReadServiceImpl(
    private val userRepository: UserRepository,
    private val fileStorageService: FileStorageService,
) : AvatarReadService {
    override suspend fun getAvatar(
        ownerId: UUID,
        loggedUserId: UUID,
    ): Resource? {
        val hasPermission =
            if (loggedUserId == ownerId) {
                true
            } else {
                userRepository
                    .findAllUsersInSameGroup(loggedUserId)
                    .collectList()
                    .map { users -> users.find { ownerId == it.id } != null }
                    .awaitSingle()
            }

        if (!hasPermission) return null

        return fileStorageService.find(AvatarStorage.key(ownerId))
    }
}
