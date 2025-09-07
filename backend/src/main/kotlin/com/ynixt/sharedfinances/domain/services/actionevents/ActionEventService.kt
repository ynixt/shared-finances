package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ActionEventService {
    fun getDestinationForUser(userId: UUID): String

    fun getDestinationForGroup(groupId: UUID): String

    fun <T> newEvent(
        userId: UUID,
        type: ActionEventType,
        category: ActionEventCategory,
        data: T,
        groupsGetter: (() -> Flux<UUID>)? = null,
    ): Mono<Long>
}
