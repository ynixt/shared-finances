package com.ynixt.sharedfinances.application.web.dto.walletentry

import java.time.LocalDate
import java.util.UUID

data class RecurrenceEventDto(
    val id: UUID,
    val qtyExecuted: Int,
    val qtyLimit: Int?,
    val lastExecution: LocalDate?,
    val nextExecution: LocalDate?,
    val endExecution: LocalDate?,
)
