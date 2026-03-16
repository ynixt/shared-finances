package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.walletentry.RecurrenceEventDto
import com.ynixt.sharedfinances.application.web.mapper.RecurrenceEventDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class RecurrenceEventDtoMapperImpl : RecurrenceEventDtoMapper {
    private object RecurrenceEventDtoMapper : ObjectMappie<RecurrenceEventEntity, RecurrenceEventDto>() {
        override fun map(from: RecurrenceEventEntity) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    override fun toDto(from: RecurrenceEventEntity): RecurrenceEventDto = RecurrenceEventDtoMapper.map(from)
}
