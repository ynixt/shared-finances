package com.ynixt.sharedfinances.application.web.dto

import java.time.LocalDate
import java.util.UUID

data class GenerateEntryRecurrenceRequestDto(
    val entryRecurrenceConfigId: UUID,
    val date: LocalDate,
)
