package com.ynixt.sharedfinances.resources.services.events.dispatchers

import com.ynixt.sharedfinances.application.web.mapper.WalletEventDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEventActionEventService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.resources.services.events.NewEventGroupInfo
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WalletEventActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: WalletEventDtoMapper,
    private val walletEventListService: WalletEventListService,
) : WalletEventActionEventService {
    override suspend fun sendInsertedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    ) = actionEventService
        .newEvent(
            data = mapper.fromListResponseToListDto(walletEventListService.convertEntityToEntryListResponse(walletEvent, true)),
            userId = userId,
            type = ActionEventType.INSERT,
            category = ActionEventCategory.WALLET_EVENT,
            groupInfo =
                if (walletEvent.groupId != null) {
                    NewEventGroupInfo(
                        groupId = walletEvent.groupId,
                    )
                } else {
                    null
                },
        )
}
