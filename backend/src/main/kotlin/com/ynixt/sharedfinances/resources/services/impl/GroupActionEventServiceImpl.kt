package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: GroupDtoMapper,
) : GroupActionEventService {
    override fun sendInsertedGroup(
        userId: UUID,
        group: Group,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = mapper.toDto(group),
                userId = userId,
                type = ActionEventType.INSERT,
                category = ActionEventCategory.GROUP,
            )
}
