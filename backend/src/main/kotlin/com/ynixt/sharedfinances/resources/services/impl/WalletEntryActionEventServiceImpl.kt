package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEntryActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.impl.NewEventGroupInfo
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WalletEntryActionEventServiceImpl(
    private val actionEventService: ActionEventService,
) : WalletEntryActionEventService {
    override suspend fun sendInsertedWalletEntry(
        userId: UUID,
        walletEntry: MinimumWalletEntry,
    ) = actionEventService
        .newEvent(
            data = walletEntry.id, // TODO
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
