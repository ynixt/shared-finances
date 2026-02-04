package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import java.time.LocalDate
import java.util.UUID

data class ListEntryRequestDto(
    val walletItemId: UUID?,
    val groupId: UUID?,
    val pageRequest: CursorPageRequest?,
    val minimumDate: LocalDate?,
    val maximumDate: LocalDate?,
    val billId: UUID?,
    val billDate: LocalDate?,
)
