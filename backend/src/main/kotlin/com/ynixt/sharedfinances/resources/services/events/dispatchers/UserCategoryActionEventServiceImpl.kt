package com.ynixt.sharedfinances.resources.services.events.dispatchers

import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.UserCategoryActionEventService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserCategoryActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: CategoryDtoMapper,
) : UserCategoryActionEventService {
    override suspend fun sendInsertedCategory(
        userId: UUID,
        category: WalletEntryCategoryEntity,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(category),
            userId = userId,
            type = ActionEventType.INSERT,
            category = ActionEventCategory.USER_CATEGORY,
        )

    override suspend fun sendUpdatedCategory(
        userId: UUID,
        category: WalletEntryCategoryEntity,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(category),
            userId = userId,
            type = ActionEventType.UPDATE,
            category = ActionEventCategory.USER_CATEGORY,
        )

    override suspend fun sendDeletedCategory(
        userId: UUID,
        id: UUID,
    ) = actionEventService
        .newEvent(
            data = id,
            userId = userId,
            type = ActionEventType.DELETE,
            category = ActionEventCategory.USER_CATEGORY,
        )
}
