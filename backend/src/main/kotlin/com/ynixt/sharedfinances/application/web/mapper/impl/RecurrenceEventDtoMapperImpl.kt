package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.walletentry.RecurrenceEventDto
import com.ynixt.sharedfinances.application.web.mapper.RecurrenceEventDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import org.springframework.stereotype.Component

@Component
class RecurrenceEventDtoMapperImpl : RecurrenceEventDtoMapper {
    private fun resolveDisplayQtyLimit(from: RecurrenceEventEntity): Int? =
        when (from.paymentType) {
            PaymentType.INSTALLMENTS -> from.seriesQtyTotal ?: from.qtyLimit
            PaymentType.RECURRING -> if (from.qtyLimit != null) from.seriesQtyTotal ?: from.qtyLimit else null
            else -> from.qtyLimit
        }

    override fun toDto(from: RecurrenceEventEntity): RecurrenceEventDto =
        RecurrenceEventDto(
            id = requireNotNull(from.id),
            paymentType = from.paymentType,
            periodicity = from.periodicity,
            qtyExecuted = from.qtyExecuted,
            qtyLimit = resolveDisplayQtyLimit(from),
            lastExecution = from.lastExecution,
            nextExecution = from.nextExecution,
            endExecution = from.endExecution,
        )
}
