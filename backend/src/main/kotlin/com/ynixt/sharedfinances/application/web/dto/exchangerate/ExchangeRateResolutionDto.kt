package com.ynixt.sharedfinances.application.web.dto.exchangerate

import java.math.BigDecimal
import java.time.LocalDate

data class ExchangeRateResolutionDto(
    val fromCurrency: String,
    val toCurrency: String,
    val referenceDate: LocalDate,
    val rate: BigDecimal?,
    val quoteDate: LocalDate?,
)
