package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.events.UserActionEventDto
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent

interface UserActionEventDtoMapper {
    fun toDto(from: UserActionEvent<Any>): UserActionEventDto
}
