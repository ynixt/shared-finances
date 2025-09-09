package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.GroupInvite
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupInviteService {
    fun generate(
        userId: UUID,
        groupId: UUID,
    ): Mono<GroupInvite>

    fun expireOld(): Mono<Long>
}
