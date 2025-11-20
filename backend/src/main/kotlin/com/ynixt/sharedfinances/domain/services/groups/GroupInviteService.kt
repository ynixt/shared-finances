package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupInviteEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupInviteService {
    fun generate(
        userId: UUID,
        groupId: UUID,
    ): Mono<GroupInviteEntity>

    fun expireOld(): Mono<Long>

    fun findInfoForInvite(inviteId: UUID): Mono<GroupInfoForInvite>

    fun accept(
        userId: UUID,
        inviteId: UUID,
    ): Mono<UUID>
}
