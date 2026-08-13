package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteDto
import com.ynixt.sharedfinances.application.web.mapper.ExchangeRateQuoteDtoMapper
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuote
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class ExchangeRateQuoteDtoMapperImpl : ExchangeRateQuoteDtoMapper {
    private object ToDtoMapper : ObjectMappie<ExchangeRateQuote, ExchangeRateQuoteDto>()

    override fun toDto(quote: ExchangeRateQuote): ExchangeRateQuoteDto = ToDtoMapper.map(quote)
}
