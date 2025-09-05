package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.models.events.GroupActionEvent
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import reactor.core.publisher.Flux
import java.util.UUID

interface ActionEventListenerService {
    fun listenUserActions(userId: UUID): Flux<UserActionEvent<Any>>

    fun listenGroupActions(groupId: UUID): Flux<GroupActionEvent<Any>>
}
