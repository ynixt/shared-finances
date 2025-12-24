package com.ynixt.sharedfinances.domain.services

import reactor.core.publisher.Mono
import java.util.UUID

interface AvatarService {
    fun getPhotoFromGravatar(
        email: String,
        userId: UUID,
    ): Mono<String>
}
