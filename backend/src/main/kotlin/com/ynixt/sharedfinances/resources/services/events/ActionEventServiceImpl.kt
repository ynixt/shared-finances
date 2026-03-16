package com.ynixt.sharedfinances.resources.services.events

import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.events.GroupActionEvent
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import io.nats.client.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.util.UUID

class NewEventGroupInfo(
    val groupId: UUID,
    var groupMemberIdGetter: (() -> Flow<UUID>)? = null,
)

@Service
class ActionEventServiceImpl(
    private val natsConnection: Connection,
    private val objectMapper: ObjectMapper,
    private val groupUsersRepository: GroupUsersRepository,
) : ActionEventService {
    // NATS usa '.' como separador hierárquico padrão, ao contrário do ':' do Redis
    override fun getDestinationForUser(userId: UUID): String = "evt.user_action.$userId"

    override fun getDestinationForGroup(userId: UUID): String = "evt.user_action.$userId.groups"

    override suspend fun <T> newEvent(
        userId: UUID,
        type: ActionEventType,
        category: ActionEventCategory,
        data: T,
        groupInfo: NewEventGroupInfo?,
    ) {
        if (groupInfo == null) {
            newUserEvent(
                UserActionEvent(
                    userId = userId,
                    type = type,
                    category = category,
                    data = data,
                ),
            )
        } else {
            if (groupInfo.groupMemberIdGetter == null) {
                groupInfo.groupMemberIdGetter = { groupUsersRepository.findAllMembers(groupInfo.groupId).map { it.userId }.asFlow() }
            }

            groupInfo.groupMemberIdGetter?.invoke()?.collect { id ->
                newGroupEvent(
                    GroupActionEvent(
                        modifiedByUserId = userId,
                        userId = id,
                        groupId = groupInfo.groupId,
                        type = type,
                        category = category,
                        data = data,
                    ),
                )
            }
        }
    }

    private suspend fun <T> newUserEvent(actionEvent: UserActionEvent<T>) {
        val destination = getDestinationForUser(actionEvent.userId)
        return publishToNats(destination, actionEvent)
    }

    private suspend fun <T> newGroupEvent(actionEvent: GroupActionEvent<T>) {
        val destination = getDestinationForGroup(actionEvent.userId)
        return publishToNats(destination, actionEvent)
    }

    private suspend fun publishToNats(
        destination: String,
        payload: Any,
    ) {
        val json = objectMapper.writeValueAsString(payload)
        natsConnection.publish(destination, json.toByteArray(StandardCharsets.UTF_8))
    }
}
