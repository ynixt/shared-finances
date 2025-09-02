package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.events.UserActionEventDto
import com.ynixt.sharedfinances.application.web.mapper.UserActionEventDtoMapper
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class UserActionEventDtoMapperImpl : UserActionEventDtoMapper {
    private object UserActionEventResponseMapper : ObjectMappie<UserActionEvent<Any>, UserActionEventDto>() {
        override fun map(from: UserActionEvent<Any>) = mapping {}
    }

    override fun toDto(from: UserActionEvent<Any>): UserActionEventDto  = UserActionEventResponseMapper.map(from)
}