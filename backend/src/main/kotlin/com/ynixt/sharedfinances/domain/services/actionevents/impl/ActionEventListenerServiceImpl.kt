package com.ynixt.sharedfinances.domain.services.actionevents.impl

import com.ynixt.sharedfinances.domain.models.events.GroupActionEvent
import com.ynixt.sharedfinances.domain.models.events.UserActionEvent
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventListenerService
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import io.nats.client.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID

@Service
class ActionEventListenerServiceImpl(
    private val natsConnection: Connection,
    private val actionEventService: ActionEventService,
    private val objectMapper: ObjectMapper,
) : ActionEventListenerService {
    override fun listenUserActions(userId: UUID): Flow<UserActionEvent<Any>> {
        val subject = actionEventService.getDestinationForUser(userId)
        return createNatsFlow(subject)
    }

    override fun listenGroupActions(userId: UUID): Flow<GroupActionEvent<Any>> {
        val subject = actionEventService.getDestinationForGroup(userId)
        return createNatsFlow(subject)
    }

    private inline fun <reified T : Any> createNatsFlow(subject: String): Flow<T> =
        flow {
            val subscription = natsConnection.subscribe(subject)

            try {
                while (currentCoroutineContext().isActive) {
                    val msg = subscription.nextMessage(Duration.ofSeconds(1))

                    if (msg != null) {
                        val json = String(msg.data, StandardCharsets.UTF_8)
                        val event = objectMapper.readValue<T>(json)
                        emit(event)
                    }
                }
            } catch (e: InterruptedException) {
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (subscription.isActive) {
                    subscription.unsubscribe()
                }
            }
        }.flowOn(Dispatchers.IO)
}
