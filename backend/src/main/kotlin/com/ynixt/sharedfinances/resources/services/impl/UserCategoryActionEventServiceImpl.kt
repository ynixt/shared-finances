package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.UserCategoryActionEventService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class UserCategoryActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: CategoryDtoMapper,
) : UserCategoryActionEventService {
    override fun sendInsertedCategory(
        userId: UUID,
        category: WalletEntryCategoryEntity,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = mapper.toDto(category),
                userId = userId,
                type = ActionEventType.INSERT,
                category = ActionEventCategory.USER_CATEGORY,
            )

    override fun sendUpdatedCategory(
        userId: UUID,
        category: WalletEntryCategoryEntity,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = mapper.toDto(category),
                userId = userId,
                type = ActionEventType.UPDATE,
                category = ActionEventCategory.USER_CATEGORY,
            )

    override fun sendDeletedCategory(
        userId: UUID,
        id: UUID,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = id,
                userId = userId,
                type = ActionEventType.DELETE,
                category = ActionEventCategory.USER_CATEGORY,
            )
}
