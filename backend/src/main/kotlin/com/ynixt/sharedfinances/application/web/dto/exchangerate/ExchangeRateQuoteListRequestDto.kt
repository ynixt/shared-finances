package com.ynixt.sharedfinances.application.web.dto.exchangerate

import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import java.time.LocalDate

data class ExchangeRateQuoteListRequestDto(
    val pageRequest: CursorPageRequest? = null,
    val baseCurrency: String? = null,
    val quoteCurrency: String? = null,
    val quoteDateFrom: LocalDate? = null,
    val quoteDateTo: LocalDate? = null,
)
