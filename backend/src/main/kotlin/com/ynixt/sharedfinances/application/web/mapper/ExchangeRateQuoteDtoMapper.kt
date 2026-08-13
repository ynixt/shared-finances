package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteDto
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuote

interface ExchangeRateQuoteDtoMapper {
    fun toDto(quote: ExchangeRateQuote): ExchangeRateQuoteDto
}
