package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.UserEntity
import java.util.UUID

interface UserActionEventService {
    suspend fun sendUpdatedUser(
        userId: UUID,
        user: UserEntity,
    )
}
