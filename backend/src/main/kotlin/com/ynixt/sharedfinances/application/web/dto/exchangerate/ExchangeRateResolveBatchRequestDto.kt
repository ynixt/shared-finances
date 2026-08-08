package com.ynixt.sharedfinances.application.web.dto.exchangerate

import java.time.LocalDate

data class ExchangeRateResolveBatchRequestDto(
    val requests: List<ExchangeRateResolveRequestDto>,
)

data class ExchangeRateResolveRequestDto(
    val fromCurrency: String,
    val toCurrency: String,
    val referenceDate: LocalDate,
)
