package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupCategoryActionEventService {
    fun sendInsertedCategory(
        userId: UUID,
        category: WalletEntryCategory,
    ): Mono<Long>

    fun sendUpdatedCategory(
        userId: UUID,
        category: WalletEntryCategory,
    ): Mono<Long>

    fun sendDeletedCategory(
        userId: UUID,
        groupId: UUID,
        id: UUID,
    ): Mono<Long>
}
