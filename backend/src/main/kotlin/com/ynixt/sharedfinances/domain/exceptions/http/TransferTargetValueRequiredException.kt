package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class TransferTargetValueRequiredException(
    fromCurrency: String,
    toCurrency: String,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.transferTargetValueRequired",
        alternativeMessage = "targetValue is required for concrete cross-currency transfer from $fromCurrency to $toCurrency",
        argsI18n =
            mapOf(
                "fromCurrency" to fromCurrency,
                "toCurrency" to toCurrency,
            ),
    )
