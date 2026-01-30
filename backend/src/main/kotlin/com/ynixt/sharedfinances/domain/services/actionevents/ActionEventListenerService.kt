package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.models.events.GroupActionEvent
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ActionEventListenerService {
    fun listenUserActions(userId: UUID): Flow<UserActionEvent<Any>>

    fun listenGroupActions(userId: UUID): Flow<GroupActionEvent<Any>>
}
