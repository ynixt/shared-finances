package com.ynixt.sharedfinances.domain.services

import java.time.Duration
import java.util.UUID

interface AvatarReadService {
    suspend fun getAvatar(
        ownerId: UUID,
        loggedUserId: UUID,
        expiresIn: Duration = Duration.ofMinutes(2),
    ): String?
}
