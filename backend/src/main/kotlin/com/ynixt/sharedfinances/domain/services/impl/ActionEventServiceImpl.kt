package com.ynixt.sharedfinances.domain.services.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.events.GroupActionEvent
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import com.ynixt.sharedfinances.domain.services.ActionEventService
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class ActionEventServiceImpl(
    private val redis: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ActionEventService {
    override fun getDestinationForUser(userId: UUID): String = "evt:user_action:$userId"

    override fun getDestinationForGroup(groupId: UUID): String = "evt:group_action:$groupId"

    override fun <T> newEvent(
        userId: UUID,
        type: ActionEventType,
        category: ActionEventCategory,
        data: T,
        groupsGetter: (() -> Flux<UUID>)?,
    ): Mono<Long> {
        val userEventMono: Mono<Long> =
            newUserEvent(
                UserActionEvent(
                    userId = userId,
                    type = type,
                    category = category,
                    data = data,
                ),
            ).defaultIfEmpty(0L)

        val groupsEventsFlux: Flux<Long> =
            groupsGetter
                ?.invoke()
                ?.flatMap { id ->
                    newGroupEvent(
                        GroupActionEvent(
                            modifiedByUserId = userId,
                            groupId = id,
                            type = type,
                            category = category,
                            data = data,
                        ),
                    ).defaultIfEmpty(0L)
                } ?: Flux.empty()

        return Flux
            .concat(userEventMono.flux(), groupsEventsFlux)
            .reduce(0L) { acc, v -> acc + v }
    }

    private fun <T> newUserEvent(actionEvent: UserActionEvent<T>): Mono<Long> {
        val destination = getDestinationForUser(actionEvent.userId)
        return redis.convertAndSend(
            destination,
            objectMapper.writeValueAsString(actionEvent),
        )
    }

    private fun <T> newGroupEvent(actionEvent: GroupActionEvent<T>): Mono<Long> {
        val destination = getDestinationForGroup(actionEvent.groupId)
        return redis.convertAndSend(
            destination,
            objectMapper.writeValueAsString(actionEvent),
        )
    }
}
