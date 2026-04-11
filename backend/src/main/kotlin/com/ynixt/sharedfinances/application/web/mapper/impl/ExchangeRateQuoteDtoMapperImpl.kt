package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteDto
import com.ynixt.sharedfinances.application.web.mapper.ExchangeRateQuoteDtoMapper
import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class ExchangeRateQuoteDtoMapperImpl : ExchangeRateQuoteDtoMapper {
    private object ToDtoMapper : ObjectMappie<ExchangeRateQuoteEntity, ExchangeRateQuoteDto>() {
        override fun map(from: ExchangeRateQuoteEntity) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    override fun toDto(entity: ExchangeRateQuoteEntity): ExchangeRateQuoteDto = ToDtoMapper.map(entity)
}
