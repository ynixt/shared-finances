package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.domain.entities.groups.Group
import com.ynixt.sharedfinances.domain.entities.groups.GroupBankAccount
import com.ynixt.sharedfinances.domain.entities.groups.GroupCreditCard
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.impl.NewEventGroupInfo
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: GroupDtoMapper,
) : GroupActionEventService {
    override fun sendInsertedGroup(
        userId: UUID,
        group: Group,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = mapper.toDto(group),
                userId = userId,
                type = ActionEventType.INSERT,
                category = ActionEventCategory.GROUP,
            )

    override fun sendUpdatedGroup(
        userId: UUID,
        group: GroupWithRole,
    ): Mono<Long> =
        actionEventService
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

    override fun sendDeletedGroup(
        userId: UUID,
        id: UUID,
        membersId: List<UUID>,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = id,
                userId = userId,
                type = ActionEventType.DELETE,
                category = ActionEventCategory.GROUP,
                groupInfo =
                    NewEventGroupInfo(
                        groupId = id,
                        groupMemberIdGetter = { Flux.fromIterable(membersId) },
                    ),
            )

    override fun sendBankAssociated(
        userId: UUID,
        groupBankAccount: GroupBankAccount,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = groupBankAccount.bankAccountId,
                userId = userId,
                type = ActionEventType.INSERT,
                category = ActionEventCategory.BANK_ACCOUNT_ASSOCIATE,
                groupInfo =
                    NewEventGroupInfo(
                        groupId = groupBankAccount.groupId,
                    ),
            )

    override fun sendBankUnassociated(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Mono<Long> =
        actionEventService
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

    override fun sendCreditCardAssociated(
        userId: UUID,
        groupCreditCard: GroupCreditCard,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = groupCreditCard.creditCardId,
                userId = userId,
                type = ActionEventType.INSERT,
                category = ActionEventCategory.CREDIT_CARD_ASSOCIATE,
                groupInfo =
                    NewEventGroupInfo(
                        groupId = groupCreditCard.groupId,
                    ),
            )

    override fun sendCreditCardUnassociated(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Mono<Long> =
        actionEventService
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
