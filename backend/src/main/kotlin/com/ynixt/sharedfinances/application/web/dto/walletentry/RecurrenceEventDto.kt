package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import java.time.LocalDate
import java.util.UUID

data class RecurrenceEventDto(
    val id: UUID,
    val paymentType: PaymentType,
    val periodicity: RecurrenceType,
    val qtyExecuted: Int,
    val qtyLimit: Int?,
    val lastExecution: LocalDate?,
    val nextExecution: LocalDate?,
    val endExecution: LocalDate?,
)
