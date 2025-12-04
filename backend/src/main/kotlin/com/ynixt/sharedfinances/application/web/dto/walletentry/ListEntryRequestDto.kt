package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import java.time.LocalDate
import java.util.UUID

data class ListEntryRequestDto(
    val walletItemIds: List<UUID>?,
    val groupId: UUID?,
    val pageRequest: CursorPageRequest?,
    val minimumDate: LocalDate?,
    val maximumDate: LocalDate?,
)
