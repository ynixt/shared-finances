package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import java.time.LocalDate

data class EditScheduledEntryDto(
    val occurrenceDate: LocalDate,
    val scope: ScheduledEditScope,
    val entry: NewEntryDto,
)
