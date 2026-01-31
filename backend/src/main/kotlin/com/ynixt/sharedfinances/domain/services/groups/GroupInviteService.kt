package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupInviteEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import java.util.UUID

interface GroupInviteService {
    suspend fun generate(
        userId: UUID,
        groupId: UUID,
    ): GroupInviteEntity?

    suspend fun expireOld(): Long

    suspend fun findInfoForInvite(inviteId: UUID): GroupInfoForInvite?

    suspend fun accept(
        userId: UUID,
        inviteId: UUID,
    ): UUID?
}
