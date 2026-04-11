package com.ynixt.sharedfinances.domain.repositories

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExchangeRateQuoteListCursor(
    val quoteDate: LocalDate,
    val baseCurrency: String,
    val quoteCurrency: String,
    val quotedAt: OffsetDateTime,
    val id: UUID,
)
