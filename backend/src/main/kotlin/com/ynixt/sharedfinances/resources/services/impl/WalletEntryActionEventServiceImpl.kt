package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEntryActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.impl.NewEventGroupInfo
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryListService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WalletEntryActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: WalletEntryDtoMapper,
    private val walletEntryListService: WalletEntryListService,
) : WalletEntryActionEventService {
    override suspend fun sendInsertedWalletEntry(
        userId: UUID,
        walletEntry: MinimumWalletEntry,
    ) = actionEventService
        .newEvent(
            data = mapper.fromListResponseToListDto(walletEntryListService.convertEntityToEntryListResponse(walletEntry, true)),
            userId = userId,
            type = ActionEventType.INSERT,
            category = ActionEventCategory.WALLET_ENTRY,
            groupInfo =
                if (walletEntry.groupId != null) {
                    NewEventGroupInfo(
                        groupId = walletEntry.groupId,
                    )
                } else {
                    null
                },
        )
}
