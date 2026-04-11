package com.ynixt.sharedfinances.application.web.dto.walletentry

import java.math.BigDecimal
import java.time.LocalDate

data class TransferRateDto(
    val rate: BigDecimal,
    val quoteDate: LocalDate,
    val baseCurrency: String,
    val quoteCurrency: String,
)
