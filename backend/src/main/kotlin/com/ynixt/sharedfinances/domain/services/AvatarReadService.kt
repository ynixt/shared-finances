package com.ynixt.sharedfinances.domain.services

import org.springframework.core.io.Resource
import java.util.UUID

interface AvatarReadService {
    suspend fun getAvatar(
        ownerId: UUID,
        loggedUserId: UUID,
    ): Resource?
}
