package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupService {
    fun findAllGroups(userId: UUID): Mono<List<Group>>

    fun findGroup(
        userId: UUID,
        id: UUID,
    ): Mono<Group>

    fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): Mono<Group>
}
