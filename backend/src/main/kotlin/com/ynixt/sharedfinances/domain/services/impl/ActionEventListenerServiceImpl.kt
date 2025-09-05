package com.ynixt.sharedfinances.domain.services.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ynixt.sharedfinances.domain.models.events.GroupActionEvent
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import com.ynixt.sharedfinances.domain.services.ActionEventListenerService
import com.ynixt.sharedfinances.domain.services.ActionEventService
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
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

    override fun listenGroupActions(groupId: UUID): Flux<GroupActionEvent<Any>> =
        container
            .receive(
                ChannelTopic(
                    actionEventService.getDestinationForGroup(groupId),
                ),
            ).map {
                objectMapper.readValue(it.message)
            }
}
