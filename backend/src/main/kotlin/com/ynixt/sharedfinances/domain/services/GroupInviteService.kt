package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.GroupInvite
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupInviteService {
    fun generate(
        userId: UUID,
        groupId: UUID,
    ): Mono<GroupInvite>

    fun expireOld(): Mono<Long>

    fun findInfoForInvite(inviteId: UUID): Mono<GroupInfoForInvite>

    fun accept(
        userId: UUID,
        inviteId: UUID,
    ): Mono<UUID>
}
