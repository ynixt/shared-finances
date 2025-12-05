package com.ynixt.sharedfinances.domain.models

import com.ynixt.sharedfinances.domain.enums.EntrySummaryType
import java.time.LocalDate
import java.util.UUID

data class SummaryEntryRequest(
    val walletItemId: UUID?,
    val groupId: UUID?,
    val minimumDate: LocalDate?,
    val maximumDate: LocalDate?,
    val summaryType: EntrySummaryType,
)
