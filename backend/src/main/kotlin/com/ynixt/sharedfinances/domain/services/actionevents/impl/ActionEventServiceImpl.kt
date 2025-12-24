package com.ynixt.sharedfinances.domain.services.actionevents.impl

import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.events.GroupActionEvent
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class NewEventGroupInfo(
    val groupId: UUID,
    var groupMemberIdGetter: (() -> Flux<UUID>)? = null,
)

@Service
class ActionEventServiceImpl(
    private val redis: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val groupUsersRepository: GroupUsersRepository,
) : ActionEventService {
    override fun getDestinationForUser(userId: UUID): String = "evt:user_action:$userId"

    override fun getDestinationForGroup(userId: UUID): String = "evt:user_action:$userId:groups"

    override fun <T> newEvent(
        userId: UUID,
        type: ActionEventType,
        category: ActionEventCategory,
        data: T,
        groupInfo: NewEventGroupInfo?,
    ): Mono<Long> {
        val userEventMono: Mono<Long> =
            if (groupInfo != null) {
                Mono.empty()
            } else {
                newUserEvent(
                    UserActionEvent(
                        userId = userId,
                        type = type,
                        category = category,
                        data = data,
                    ),
                ).defaultIfEmpty(0L)
            }

        if (groupInfo != null && groupInfo.groupMemberIdGetter == null) {
            groupInfo.groupMemberIdGetter = { groupUsersRepository.findAllMembers(groupInfo.groupId).map { it.userId } }
        }

        val groupsEventsFlux: Flux<Long> =
            groupInfo?.groupMemberIdGetter?.invoke()?.flatMap { id ->
                newGroupEvent(
                    GroupActionEvent(
                        modifiedByUserId = userId,
                        userId = id,
                        groupId = groupInfo.groupId,
                        type = type,
                        category = category,
                        data = data,
                    ),
                ).defaultIfEmpty(0L)
            }
                ?: Flux.empty()

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
        val destination = getDestinationForGroup(actionEvent.userId)
        return redis.convertAndSend(
            destination,
            objectMapper.writeValueAsString(actionEvent),
        )
    }
}
