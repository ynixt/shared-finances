package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.Group
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupActionEventService {
    fun sendInsertedGroup(
        userId: UUID,
        group: Group,
    ): Mono<Long>
}
