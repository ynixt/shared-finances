package com.ynixt.sharedfinances.resources.services

import java.util.UUID

internal object AvatarStorage {
    fun key(userId: UUID): String = "avatars/$userId.png"

    fun publicRoute(userId: UUID): String = "/api/private/avatars/$userId"
}
