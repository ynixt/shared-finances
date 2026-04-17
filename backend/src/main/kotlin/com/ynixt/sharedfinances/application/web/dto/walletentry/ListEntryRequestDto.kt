package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import java.time.LocalDate
import java.util.UUID

data class ListEntryRequestDto(
    val walletItemId: UUID?,
    val groupIds: List<UUID>?,
    val creditCardIds: List<UUID>?,
    val userIds: List<UUID>?,
    val bankAccountIds: List<UUID>?,
    val entryTypes: List<WalletEntryType>?,
    val pageRequest: CursorPageRequest?,
    val minimumDate: LocalDate?,
    val maximumDate: LocalDate?,
    val billId: UUID?,
    val billDate: LocalDate?,
)
