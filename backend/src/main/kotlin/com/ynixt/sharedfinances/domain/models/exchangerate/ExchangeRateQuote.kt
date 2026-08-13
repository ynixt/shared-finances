package com.ynixt.sharedfinances.domain.models.exchangerate

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class ExchangeRateQuote(
    val source: String,
    val baseCurrency: String,
    val quoteCurrency: String,
    val quoteDate: LocalDate,
    val rate: BigDecimal,
    val fetchedAt: OffsetDateTime,
    val derived: Boolean,
)

data class ExchangeRateQuotePairRates(
    val quoteDate: LocalDate,
    val rateFrom: BigDecimal,
    val rateTo: BigDecimal,
)
