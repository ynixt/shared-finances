package com.ynixt.sharedfinances.application.web.dto.exchangerate

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExchangeRateQuoteDto(
    val id: UUID,
    val source: String,
    val baseCurrency: String,
    val quoteCurrency: String,
    val quoteDate: LocalDate,
    val rate: BigDecimal,
    val quotedAt: OffsetDateTime,
    val fetchedAt: OffsetDateTime,
)
