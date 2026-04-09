package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import java.time.LocalDate

data class EditScheduledEntryRequest(
    val occurrenceDate: LocalDate,
    val scope: ScheduledEditScope,
    val entry: NewEntryRequest,
)
