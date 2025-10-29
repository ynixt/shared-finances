package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.impl.NewEventGroupInfo
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupCategoryActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: CategoryDtoMapper,
) : GroupCategoryActionEventService {
    override fun sendInsertedCategory(
        userId: UUID,
        category: WalletEntryCategory,
    ): Mono<Long> =
        actionEventService
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

    override fun sendUpdatedCategory(
        userId: UUID,
        category: WalletEntryCategory,
    ): Mono<Long> =
        actionEventService
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

    override fun sendDeletedCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
    ): Mono<Long> =
        actionEventService
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
