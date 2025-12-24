package com.ynixt.sharedfinances.domain.services

import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

interface AvatarReadService {
    fun getAvatar(
        ownerId: UUID,
        loggedUserId: UUID,
        expiresIn: Duration = Duration.ofMinutes(2),
    ): Mono<String>
}
