package com.ynixt.sharedfinances.resources.services.events.dispatchers

import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import kotlinx.coroutines.flow.asFlow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: GroupDtoMapper,
) : GroupActionEventService {
    override suspend fun sendInsertedGroup(
        userId: UUID,
        group: GroupEntity,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(group),
            userId = userId,
            type = ActionEventType.INSERT,
            category = ActionEventCategory.GROUP,
        )

    override suspend fun sendUpdatedGroup(
        userId: UUID,
        group: GroupWithRole,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(group),
            userId = userId,
            type = ActionEventType.UPDATE,
            category = ActionEventCategory.GROUP,
            groupInfo =
                NewEventGroupInfo(
                    groupId = group.id!!,
                ),
        )

    override suspend fun sendDeletedGroup(
        userId: UUID,
        id: UUID,
        membersId: List<UUID>,
    ) = actionEventService
        .newEvent(
            data = id,
            userId = userId,
            type = ActionEventType.DELETE,
            category = ActionEventCategory.GROUP,
            groupInfo =
                NewEventGroupInfo(
                    groupId = id,
                    groupMemberIdGetter = { membersId.asFlow() },
                ),
        )

    override suspend fun sendBankAssociated(
        userId: UUID,
        groupBankAccount: GroupWalletItemEntity,
    ) = actionEventService
        .newEvent(
            data = groupBankAccount.walletItemId,
            userId = userId,
            type = ActionEventType.INSERT,
            category = ActionEventCategory.BANK_ACCOUNT_ASSOCIATE,
            groupInfo =
                NewEventGroupInfo(
                    groupId = groupBankAccount.groupId,
                ),
        )

    override suspend fun sendBankUnassociated(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ) = actionEventService
        .newEvent(
            data = bankAccountId,
            userId = userId,
            type = ActionEventType.DELETE,
            category = ActionEventCategory.BANK_ACCOUNT_ASSOCIATE,
            groupInfo =
                NewEventGroupInfo(
                    groupId = groupId,
                ),
        )

    override suspend fun sendCreditCardAssociated(
        userId: UUID,
        groupCreditCard: GroupWalletItemEntity,
    ) = actionEventService
        .newEvent(
            data = groupCreditCard.walletItemId,
            userId = userId,
            type = ActionEventType.INSERT,
            category = ActionEventCategory.CREDIT_CARD_ASSOCIATE,
            groupInfo =
                NewEventGroupInfo(
                    groupId = groupCreditCard.groupId,
                ),
        )

    override suspend fun sendCreditCardUnassociated(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ) = actionEventService
        .newEvent(
            data = creditCardId,
            userId = userId,
            type = ActionEventType.DELETE,
            category = ActionEventCategory.CREDIT_CARD_ASSOCIATE,
            groupInfo =
                NewEventGroupInfo(
                    groupId = groupId,
                ),
        )
}
