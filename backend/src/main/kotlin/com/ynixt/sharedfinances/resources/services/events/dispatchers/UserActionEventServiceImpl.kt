package com.ynixt.sharedfinances.resources.services.events.dispatchers

import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.UserActionEventService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: UserDtoMapper,
) : UserActionEventService {
    override suspend fun sendUpdatedUser(
        userId: UUID,
        user: UserEntity,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(user),
            userId = userId,
            type = ActionEventType.UPDATE,
            category = ActionEventCategory.USER,
        )
}
