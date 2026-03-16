package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.walletentry.RecurrenceEventDto
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity

interface RecurrenceEventDtoMapper {
    fun toDto(from: RecurrenceEventEntity): RecurrenceEventDto
}
