package com.ynixt.sharedfinances.resources.repositories.redis

import java.util.UUID

internal object AvatarRedisKeys {
    fun avatarPresignedUrl(ownerId: UUID): String = "sf:avatar:presigned:$ownerId"
}
