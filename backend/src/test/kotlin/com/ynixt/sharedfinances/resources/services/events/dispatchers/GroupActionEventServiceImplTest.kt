package com.ynixt.sharedfinances.resources.services.events.dispatchers

import com.ynixt.sharedfinances.application.web.dto.groups.GroupOwnershipChangedEventDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupUpdatedEventDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

class GroupActionEventServiceImplTest {
    private val recorder = RecordingActionEventService()
    private val service = GroupActionEventServiceImpl(recorder, Mockito.mock(GroupDtoMapper::class.java))

    @Test
    fun `group update contains only editable group facts`() =
        runTest {
            val groupId = UUID.randomUUID()

            service.sendUpdatedGroup(UUID.randomUUID(), groupId, "Renamed")

            assertThat(recorder.events).hasSize(1)
            assertThat(recorder.events.single().data).isEqualTo(GroupUpdatedEventDto(groupId, "Renamed"))
            assertThat(recorder.events.single().category).isEqualTo(ActionEventCategory.GROUP)
        }

    @Test
    fun `ownership event names both parties in one group event`() =
        runTest {
            val groupId = UUID.randomUUID()
            val previousOwnerId = UUID.randomUUID()
            val newOwnerId = UUID.randomUUID()

            service.sendOwnershipChanged(UUID.randomUUID(), groupId, previousOwnerId, newOwnerId)

            assertThat(recorder.events).hasSize(1)
            assertThat(recorder.events.single().data)
                .isEqualTo(GroupOwnershipChangedEventDto(groupId, previousOwnerId, newOwnerId))
            assertThat(recorder.events.single().category).isEqualTo(ActionEventCategory.GROUP_OWNERSHIP)
            assertThat(
                recorder.events
                    .single()
                    .groupInfo
                    ?.groupId,
            ).isEqualTo(groupId)
        }

    private class RecordingActionEventService : ActionEventService {
        data class RecordedEvent(
            val type: ActionEventType,
            val category: ActionEventCategory,
            val data: Any?,
            val groupInfo: NewEventGroupInfo?,
        )

        val events = mutableListOf<RecordedEvent>()

        override fun getDestinationForUser(userId: UUID): String = userId.toString()

        override fun getDestinationForGroup(userId: UUID): String = userId.toString()

        override suspend fun <T> newEvent(
            userId: UUID,
            type: ActionEventType,
            category: ActionEventCategory,
            data: T,
            groupInfo: NewEventGroupInfo?,
        ) {
            events += RecordedEvent(type, category, data, groupInfo)
        }
    }
}
