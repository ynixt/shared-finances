package com.ynixt.sharedfinances.resources.services.events

import com.ynixt.sharedfinances.application.web.dto.events.GroupPlanUsageEventDto
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import io.nats.client.Connection
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import reactor.core.publisher.Flux
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.test.assertEquals

class ActionEventServiceImplGroupUsageTest {
    @Test
    fun `group usage payload reaches every member with identical quota facts`() =
        runTest {
            val groupId = UUID.randomUUID()
            val actorId = UUID.randomUUID()
            val memberIds = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
            val members = Mockito.mock(GroupUsersRepository::class.java)
            Mockito.`when`(members.findAllMembers(groupId)).thenReturn(
                Flux.fromIterable(memberIds.map { GroupUserEntity(groupId, it, UserGroupRole.VIEWER) }),
            )
            val connection = Mockito.mock(Connection::class.java)
            val mapper = jacksonObjectMapper()
            val service = ActionEventServiceImpl(connection, mapper, members)
            val payload = GroupPlanUsageEventDto(groupId, PlanLimitKey.GROUP_GOALS, 8)

            service.newEvent(
                actorId,
                ActionEventType.UPDATE,
                ActionEventCategory.GROUP_PLAN_USAGE,
                payload,
                NewEventGroupInfo(groupId),
            )

            val destination = ArgumentCaptor.forClass(String::class.java)
            val body = ArgumentCaptor.forClass(ByteArray::class.java)
            Mockito.verify(connection, Mockito.times(memberIds.size)).publish(destination.capture(), body.capture())
            assertEquals(memberIds.map(service::getDestinationForGroup).toSet(), destination.allValues.toSet())
            val dataNodes =
                body.allValues.map {
                    mapper.readTree(String(it, StandardCharsets.UTF_8)).get("data")
                }
            assertEquals(1, dataNodes.toSet().size)
            val facts = dataNodes.first()
            assertEquals(groupId.toString(), facts.get("groupId").stringValue())
            assertEquals(PlanLimitKey.GROUP_GOALS.name, facts.get("quota").stringValue())
            assertEquals(8, facts.get("usage").intValue())
        }
}
