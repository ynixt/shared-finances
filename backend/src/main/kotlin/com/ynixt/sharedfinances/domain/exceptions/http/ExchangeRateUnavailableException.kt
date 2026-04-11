package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus
import java.time.LocalDate

class ExchangeRateUnavailableException(
    fromCurrency: String,
    toCurrency: String,
    referenceDate: LocalDate,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.exchangeRateUnavailable",
        alternativeMessage = "No exchange rate available from $fromCurrency to $toCurrency near $referenceDate",
        argsI18n =
            mapOf(
                "fromCurrency" to fromCurrency,
                "toCurrency" to toCurrency,
                "referenceDate" to referenceDate.toString(),
            ),
    )
