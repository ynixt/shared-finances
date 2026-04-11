package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteDto
import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity

interface ExchangeRateQuoteDtoMapper {
    fun toDto(entity: ExchangeRateQuoteEntity): ExchangeRateQuoteDto
}
