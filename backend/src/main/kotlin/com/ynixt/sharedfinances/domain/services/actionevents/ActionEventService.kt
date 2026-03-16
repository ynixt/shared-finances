package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import java.util.UUID

interface ActionEventService {
    fun getDestinationForUser(userId: UUID): String

    fun getDestinationForGroup(userId: UUID): String

    suspend fun <T> newEvent(
        userId: UUID,
        type: ActionEventType,
        category: ActionEventCategory,
        data: T,
        groupInfo: NewEventGroupInfo? = null,
    )
}
