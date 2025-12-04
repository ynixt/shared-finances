package com.ynixt.sharedfinances.domain.models

import java.time.LocalDate
import java.util.UUID

data class ListEntryRequest(
    val walletItemIds: List<UUID>?,
    val groupId: UUID?,
    val pageRequest: CursorPageRequest = CursorPageRequest(),
    val minimumDate: LocalDate?,
    val maximumDate: LocalDate?,
) {
    val lastId: UUID? = (pageRequest.nextCursor?.get("id") as String?)?.let { UUID.fromString(it) }
    val lastDate: LocalDate? = (pageRequest.nextCursor?.get("date") as String?)?.let { LocalDate.parse(it) }

    init {

        require(
            lastId == null && lastDate == null || lastId != null && lastDate != null,
        ) { "When sending a \"last\" you need to send both." }
    }
}
