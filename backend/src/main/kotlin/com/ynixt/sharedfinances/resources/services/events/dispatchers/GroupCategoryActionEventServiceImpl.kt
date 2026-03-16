package com.ynixt.sharedfinances.resources.services.events.dispatchers

import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupCategoryActionEventService
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupCategoryActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: CategoryDtoMapper,
) : GroupCategoryActionEventService {
    override suspend fun sendInsertedCategory(
        userId: UUID,
        category: WalletEntryCategoryEntity,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(category),
            userId = userId,
            groupInfo =
                NewEventGroupInfo(
                    groupId = category.groupId!!,
                ),
            type = ActionEventType.INSERT,
            category = ActionEventCategory.GROUP_CATEGORY,
        )

    override suspend fun sendUpdatedCategory(
        userId: UUID,
        category: WalletEntryCategoryEntity,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(category),
            userId = userId,
            groupInfo =
                NewEventGroupInfo(
                    groupId = category.groupId!!,
                ),
            type = ActionEventType.UPDATE,
            category = ActionEventCategory.GROUP_CATEGORY,
        )

    override suspend fun sendDeletedCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
    ) = actionEventService
        .newEvent(
            data = id,
            userId = userId,
            groupInfo =
                NewEventGroupInfo(
                    groupId = groupId,
                ),
            type = ActionEventType.DELETE,
            category = ActionEventCategory.GROUP_CATEGORY,
        )
}
