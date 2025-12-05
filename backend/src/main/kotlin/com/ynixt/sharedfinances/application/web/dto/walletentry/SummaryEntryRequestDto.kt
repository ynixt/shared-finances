package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.enums.EntrySummaryType
import java.time.LocalDate
import java.util.UUID

data class SummaryEntryRequestDto(
    val walletItemId: UUID?,
    val groupId: UUID?,
    val minimumDate: LocalDate?,
    val maximumDate: LocalDate?,
    val summaryType: EntrySummaryType,
)
