package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupActionEventService {
    fun sendInsertedGroup(
        userId: UUID,
        group: Group,
    ): Mono<Long>

    fun sendUpdatedGroup(
        userId: UUID,
        group: GroupWithRole,
    ): Mono<Long>

    fun sendDeletedGroup(
        userId: UUID,
        id: UUID,
        membersId: List<UUID>,
    ): Mono<Long>
}
