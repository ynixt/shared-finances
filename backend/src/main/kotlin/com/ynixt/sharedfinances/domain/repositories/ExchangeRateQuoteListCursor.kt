package com.ynixt.sharedfinances.domain.repositories

import java.time.LocalDate

data class ExchangeRateQuoteListCursor(
    val quoteDate: LocalDate,
    val baseCurrency: String,
    val quoteCurrency: String,
)
