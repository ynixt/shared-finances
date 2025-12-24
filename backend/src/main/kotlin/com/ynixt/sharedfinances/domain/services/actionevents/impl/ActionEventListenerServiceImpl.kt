package com.ynixt.sharedfinances.domain.services.actionevents.impl

import com.ynixt.sharedfinances.domain.models.events.GroupActionEvent
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventListenerService
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Service
class ActionEventListenerServiceImpl(
    private val container: ReactiveRedisMessageListenerContainer,
    private val actionEventService: ActionEventService,
    private val objectMapper: ObjectMapper,
) : ActionEventListenerService {
    override fun listenUserActions(userId: UUID): Flux<UserActionEvent<Any>> =
        container
            .receive(
                ChannelTopic(
                    actionEventService.getDestinationForUser(userId),
                ),
            ).map {
                objectMapper.readValue(it.message)
            }

    override fun listenGroupActions(userId: UUID): Flux<GroupActionEvent<Any>> =
        container
            .receive(
                ChannelTopic(
                    actionEventService.getDestinationForGroup(userId),
                ),
            ).map {
                objectMapper.readValue(it.message)
            }
}
