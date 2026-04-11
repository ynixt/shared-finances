package com.ynixt.sharedfinances.domain.models.exchangerate

import com.ynixt.sharedfinances.domain.exceptions.http.InvalidExchangeRateQuoteCursorException
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.repositories.ExchangeRateQuoteListCursor
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExchangeRateQuoteListRequest(
    val pageRequest: CursorPageRequest,
    val baseCurrency: String?,
    val quoteCurrency: String?,
    val quoteDateFrom: LocalDate?,
    val quoteDateTo: LocalDate?,
) {
    val cursor: ExchangeRateQuoteListCursor? =
        pageRequest.nextCursor?.let { m ->
            val quoteDateStr = m["quoteDate"] as? String
            val baseCurrencyStr = m["baseCurrency"] as? String
            val quoteCurrencyStr = m["quoteCurrency"] as? String
            val quotedAtStr = m["quotedAt"] as? String
            val idStr = m["id"] as? String
            when {
                quoteDateStr == null &&
                    baseCurrencyStr == null &&
                    quoteCurrencyStr == null &&
                    quotedAtStr == null &&
                    idStr == null -> null
                quoteDateStr != null &&
                    baseCurrencyStr != null &&
                    quoteCurrencyStr != null &&
                    quotedAtStr != null &&
                    idStr != null ->
                    try {
                        ExchangeRateQuoteListCursor(
                            quoteDate = LocalDate.parse(quoteDateStr),
                            baseCurrency = baseCurrencyStr.uppercase(),
                            quoteCurrency = quoteCurrencyStr.uppercase(),
                            quotedAt = OffsetDateTime.parse(quotedAtStr),
                            id = UUID.fromString(idStr),
                        )
                    } catch (_: RuntimeException) {
                        throw InvalidExchangeRateQuoteCursorException(
                            "nextCursor for exchange rate quotes is invalid.",
                        )
                    }
                else ->
                    throw InvalidExchangeRateQuoteCursorException(
                        "nextCursor for exchange rate quotes must include quoteDate, baseCurrency, quoteCurrency, quotedAt, and id together, or omit all.",
                    )
            }
        }

    init {
        if (quoteDateFrom != null && quoteDateTo != null) {
            require(!quoteDateFrom.isAfter(quoteDateTo)) { "quoteDateFrom cannot be after quoteDateTo" }
        }
    }
}
