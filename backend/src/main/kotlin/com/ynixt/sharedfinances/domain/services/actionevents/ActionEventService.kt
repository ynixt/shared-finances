package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.impl.NewEventGroupInfo
import reactor.core.publisher.Mono
import java.util.UUID

interface ActionEventService {
    fun getDestinationForUser(userId: UUID): String

    fun getDestinationForGroup(userId: UUID): String

    fun <T> newEvent(
        userId: UUID,
        type: ActionEventType,
        category: ActionEventCategory,
        data: T,
        groupInfo: NewEventGroupInfo? = null,
    ): Mono<Long>
}
