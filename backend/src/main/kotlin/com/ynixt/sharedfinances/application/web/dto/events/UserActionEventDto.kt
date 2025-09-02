package com.ynixt.sharedfinances.application.web.dto.events

import com.ynixt.sharedfinances.domain.enums.ActionEventType
import java.util.UUID

data class UserActionEventDto(
    val id: UUID = UUID.randomUUID(),
    val type: ActionEventType,
    val data: Any,
)