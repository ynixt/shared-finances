package com.ynixt.sharedfinances.domain.models

import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import java.time.LocalDate
import java.util.UUID

data class ListEntryRequest(
    val walletItemId: UUID?,
    val groupIds: Set<UUID> = emptySet(),
    val userIds: Set<UUID> = emptySet(),
    val creditCardIds: Set<UUID> = emptySet(),
    val bankAccountIds: Set<UUID> = emptySet(),
    val entryTypes: Set<WalletEntryType> = emptySet(),
    val pageRequest: CursorPageRequest,
    val minimumDate: LocalDate?,
    val maximumDate: LocalDate?,
    val billId: UUID?,
    val billDate: LocalDate?,
) {
    val lastId: UUID? = (pageRequest.nextCursor?.get("id") as String?)?.let { UUID.fromString(it) }
    val lastDate: LocalDate? = (pageRequest.nextCursor?.get("date") as String?)?.let { LocalDate.parse(it) }
    val skipFuture: Boolean = pageRequest.nextCursor?.get("skipFuture") as Boolean? ?: false

    init {
        require(
            lastId == null && lastDate == null || lastId != null && lastDate != null,
        ) { "When sending a \"last\" you need to send both." }
        require(
            groupIds.isNotEmpty() || userIds.isEmpty(),
        ) { "Filter userIds requires at least one groupId." }
    }
}
